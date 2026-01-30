package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.hibernate.Criteria;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.kenyaemr.cashier.api.BillLineItemService;
import org.openmrs.module.kenyaemr.cashier.api.IBillableItemsService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.base.f.Action1;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItemAdjustment;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableService;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableServiceStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableServiceTax;
import org.openmrs.module.kenyaemr.cashier.api.model.CashierTaxType;
import org.openmrs.api.context.Context;
import org.openmrs.OpenmrsObject;
import org.openmrs.module.kenyaemr.cashier.api.search.BillItemSearch;
import org.openmrs.module.kenyaemr.cashier.api.search.BillableServiceSearch;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Transactional
public class BillLineItemServiceImpl extends BaseEntityDataServiceImpl<BillLineItem>
        implements IEntityAuthorizationPrivileges
        , BillLineItemService {

	private static final Log LOG = LogFactory.getLog(BillLineItemServiceImpl.class);

	@Override
	protected IEntityAuthorizationPrivileges getPrivileges() {
		return this;
	}

	@Override
	public List<BillLineItem> fetchBillItemByOrder(final BillItemSearch billItemSearch) {
		if (billItemSearch == null) {
			throw new NullPointerException("The bill item search must be defined.");
		} else if (billItemSearch.getTemplate() == null) {
			throw new NullPointerException("The bill item search template must be defined.");
		}
		return executeCriteria(BillLineItem.class, null, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				billItemSearch.updateCriteria(criteria);
			}
		});
	}

	@Override
	protected void validate(BillLineItem object) {

	}

	@Override
	public BillLineItem save(BillLineItem object) {
		applyTaxes(object);
		return super.save(object);
	}

	@Override
	public BillLineItem saveAll(BillLineItem object, Collection<? extends OpenmrsObject> related) {
		applyTaxes(object);
		return super.saveAll(object, related);
	}

	@Override
	protected Collection<? extends OpenmrsObject> getRelatedObjects(BillLineItem entity) {
		List<OpenmrsObject> related = new ArrayList<>();
		if (entity == null) {
			return related;
		}
		if (entity.getAdjustments() != null) {
			related.addAll(entity.getAdjustments());
		}
		return related;
	}

	private void applyTaxes(BillLineItem item) {
		if (item == null) {
			LOG.info("applyTaxes: item is null, skipping");
			return;
		}
		LOG.info("applyTaxes: processing item uuid=" + item.getUuid() +
				" billableService=" + (item.getBillableService() != null ? item.getBillableService().getUuid() : "null") +
				" itemPrice=" + (item.getItemPrice() != null ? item.getItemPrice().getUuid() : "null") +
				" stockItem=" + (item.getItem() != null ? item.getItem().getUuid() : "null"));

		// Try to resolve billableService from various sources
		IBillableItemsService billableItemsService = Context.getService(IBillableItemsService.class);
		if (item.getBillableService() == null) {
			// First, try to resolve from itemPrice (CashierItemPrice has billableService reference)
			if (item.getItemPrice() != null && item.getItemPrice().getBillableService() != null) {
				BillableService serviceFromPrice = item.getItemPrice().getBillableService();
				// Re-fetch the BillableService to ensure serviceTaxes collection is properly loaded
				BillableService fullService = billableItemsService.getByUuid(serviceFromPrice.getUuid());
				if (fullService != null) {
					item.setBillableService(fullService);
					LOG.info("applyTaxes: resolved billableService from itemPrice (re-fetched)=" + fullService.getUuid());
				} else {
					item.setBillableService(serviceFromPrice);
					LOG.info("applyTaxes: resolved billableService from itemPrice (proxy)=" + serviceFromPrice.getUuid());
				}
			}
			// Second, try to resolve from stock item
			else if (item.getItem() != null) {
				BillableService service = resolveBillableService(item);
				if (service != null) {
					item.setBillableService(service);
					LOG.info("applyTaxes: resolved billableService from stockItem=" + service.getUuid());
				} else {
					LOG.info("applyTaxes: no billableService found for stock item " + item.getItem().getUuid());
				}
			}
		} else {
			// BillableService is already set, but might be a proxy - re-fetch to ensure serviceTaxes are loaded
			BillableService fullService = billableItemsService.getByUuid(item.getBillableService().getUuid());
			if (fullService != null) {
				item.setBillableService(fullService);
				LOG.info("applyTaxes: re-fetched existing billableService=" + fullService.getUuid());
			}
		}
		if (item.getBillableService() == null) {
			LOG.info("applyTaxes: billableService is null, skipping tax calculation");
			return;
		}
		if (item.getPrice() == null || item.getQuantity() == null) {
			LOG.info("applyTaxes: missing price or quantity, skipping. price=" + item.getPrice()
					+ " quantity=" + item.getQuantity());
			return;
		}

		BigDecimal baseAmount = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
		if (baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
			LOG.info("applyTaxes: baseAmount <= 0, skipping. baseAmount=" + baseAmount);
			return;
		}

		List<BillLineItemAdjustment> adjustments = item.getAdjustments();
		if (adjustments == null) {
			adjustments = new ArrayList<>();
			item.setAdjustments(adjustments);
			LOG.info("applyTaxes: created adjustments list");
		} else {
			Iterator<BillLineItemAdjustment> iterator = adjustments.iterator();
			int removed = 0;
			while (iterator.hasNext()) {
				BillLineItemAdjustment existing = iterator.next();
				if (existing != null && "TAX".equalsIgnoreCase(existing.getAdjustmentType())) {
					iterator.remove();
					removed++;
				}
			}
			if (removed > 0) {
				LOG.info("applyTaxes: removed existing TAX adjustments=" + removed);
			}
		}

		BigDecimal discountAmount = calculateDiscountAmount(adjustments);
		BigDecimal taxableAmount = baseAmount.subtract(discountAmount);
		if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) {
			taxableAmount = BigDecimal.ZERO;
		}
		LOG.info("applyTaxes: baseAmount=" + baseAmount + " discountAmount=" + discountAmount
				+ " taxableAmount=" + taxableAmount);
		if (taxableAmount.compareTo(BigDecimal.ZERO) <= 0) {
			LOG.info("applyTaxes: taxableAmount <= 0, skipping tax generation");
			return;
		}

		List<BillableServiceTax> serviceTaxes = item.getBillableService().getServiceTaxes();
		if (serviceTaxes == null) {
			LOG.info("applyTaxes: no serviceTaxes for billableService=" + item.getBillableService().getUuid());
			return;
		}
		LOG.info("applyTaxes: serviceTaxes count=" + serviceTaxes.size());
		for (BillableServiceTax serviceTax : serviceTaxes) {
			if (serviceTax == null || serviceTax.getVoided()) {
				LOG.info("applyTaxes: skipping voided or null serviceTax");
				continue;
			}
			CashierTaxType taxType = serviceTax.getTaxType();
			BigDecimal rate = serviceTax.getOverrideRate() != null ? serviceTax.getOverrideRate()
					: (taxType == null ? null : taxType.getRate());
			if (rate == null) {
				LOG.info("applyTaxes: missing rate for serviceTax id=" + serviceTax.getId());
				continue;
			}

			BigDecimal amount = taxableAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
			BillLineItemAdjustment adjustment = new BillLineItemAdjustment();
			adjustment.setAdjustmentType("TAX");
			adjustment.setTaxType(taxType);
			adjustment.setRate(rate);
			adjustment.setAmount(amount);
			adjustment.setBaseAmount(taxableAmount);
			adjustment.setBillLineItem(item);
			// Set required BaseOpenmrsData fields for Hibernate persistence
			adjustment.setCreator(Context.getAuthenticatedUser());
			adjustment.setDateCreated(new Date());
			adjustment.setVoided(false);
			adjustment.setUuid(UUID.randomUUID().toString());
			adjustments.add(adjustment);
			LOG.info("applyTaxes: added TAX adjustment amount=" + amount + " rate=" + rate + " uuid=" + adjustment.getUuid());
		}
	}

	private BigDecimal calculateDiscountAmount(List<BillLineItemAdjustment> adjustments) {
		BigDecimal discountAmount = BigDecimal.ZERO;
		if (adjustments == null) {
			return discountAmount;
		}
		for (BillLineItemAdjustment adjustment : adjustments) {
			if (adjustment == null || adjustment.getVoided()) {
				continue;
			}
			if (!"DISCOUNT".equalsIgnoreCase(adjustment.getAdjustmentType())) {
				continue;
			}
			BigDecimal amount = adjustment.getAmount();
			if (amount == null && adjustment.getBaseAmount() != null && adjustment.getRate() != null) {
				amount = adjustment.getBaseAmount().multiply(adjustment.getRate());
			}
			if (amount != null) {
				discountAmount = discountAmount.add(amount);
			}
		}
		return discountAmount;
	}

	private BillableService resolveBillableService(BillLineItem item) {
		if (item == null || item.getItem() == null) {
			return null;
		}
		BillableService template = new BillableService();
		template.setStockItem(item.getItem());
		template.setServiceStatus(BillableServiceStatus.ENABLED);
		IBillableItemsService service = Context.getService(IBillableItemsService.class);
		List<BillableService> results = service.findServices(new BillableServiceSearch(template));
		return results == null || results.isEmpty() ? null : results.get(0);
	}

	@Override
	public String getVoidPrivilege() {
		return null;
	}

	@Override
	public String getSavePrivilege() {
		return null;
	}

	@Override
	public String getPurgePrivilege() {
		return null;
	}

	@Override
	public String getGetPrivilege() {
		return null;
	}
}
