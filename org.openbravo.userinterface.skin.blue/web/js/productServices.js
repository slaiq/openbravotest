OB.ProductServices=OB.ProductServices||{};OB.ProductServices.rfcWindowId="FF808081330213E60133021822E40007";OB.ProductServices.onLoad=function(a){var b=a.theForm.getItem("grid").canvas.viewGrid;b.selectionChanged=OB.ProductServices.relateOrderLinesSelectionChanged;};OB.ProductServices.onLoadGrid=function(a){OB.ProductServices.updateTotalLinesAmount(this.view.theForm);
OB.ProductServices.updateServicePrice(this.view,null,null);};OB.ProductServices.updateTotalLinesAmount=function(c){var f=BigDecimal.prototype.ZERO,m=BigDecimal.prototype.ZERO,d=BigDecimal.prototype.ZERO,g=BigDecimal.prototype.ZERO,b=BigDecimal.prototype.ZERO,a=c.getItem("grid").canvas.viewGrid,e=a.getFieldByColumnName("amount"),k=a.getFieldByColumnName("discountsAmt"),q=a.getFieldByColumnName("price"),h=a.getFieldByColumnName("relatedQuantity"),l=a.getFieldByColumnName("unitDiscountsAmt"),o=a.getSelectedRecords(),r=c.getItem("totallinesamount"),v=c.getItem("totaldiscountsamount"),u=c.getItem("totapriceamount"),s=c.getItem("totalrelatedqty"),j=c.getItem("totalUnitDiscountsAmt"),n,p,w,t;
for(n=0;n<o.length;n++){p=new BigDecimal(String(a.getEditedCell(a.getRecordIndex(o[n]),e)));w=new BigDecimal(String(a.getEditedCell(a.getRecordIndex(o[n]),k)));t=new BigDecimal(String(a.getEditedCell(a.getRecordIndex(o[n]),q)));lineRelatedQty=new BigDecimal(String(a.getEditedCell(a.getRecordIndex(o[n]),h)));
lineUnitDiscountAmt=new BigDecimal(String(a.getEditedCell(a.getRecordIndex(o[n]),l)));f=f.add(p);m=m.add(w);d=d.add(t);g=g.add(lineRelatedQty);b=b.add(lineUnitDiscountAmt);}r.setValue(Number(f.toString()));v.setValue(Number(m.toString()));u.setValue(Number(d.toString()));s.setValue(Number(g.toString()));
j.setValue(Number(b.toString()));return true;};OB.ProductServices.orderLinesGridQtyOnChange=function(k,h,b,a){var e=a.getEditValues(a.getRecordIndex(k.record)).amount,f=new BigDecimal(String(k.getValue())).multiply(new BigDecimal(String(k.record.price))),i=new BigDecimal(String(k.record.originalOrderedQuantity)),j=new BigDecimal(String(k.getValue())),c=a.getEditValues(a.getRecordIndex(k.record)).discountsAmount,g=new BigDecimal(String(k.getValue())).multiply(new BigDecimal(String(k.record.unitDiscountsAmt))),d=b.getItem("pricePrecision").getValue();
f=f.setScale(d,BigDecimal.prototype.ROUND_HALF_UP);g=g.setScale(d,BigDecimal.prototype.ROUND_HALF_UP);a.setEditValue(a.getRecordIndex(k.record),"amount",Number(f));a.setEditValue(a.getRecordIndex(k.record),"discountsAmount",Number(g));OB.ProductServices.updateTotalLinesAmount(b);OB.ProductServices.updateServicePrice(h,k.record,null);
};OB.ProductServices.QuantityValidate=function(e,c,g,a){if(!isc.isA.Number(g)){return false;}if(g===null){return false;}var h=new BigDecimal(String(g)),f=new BigDecimal(String(a.returnQtyOtherRM)),d=new BigDecimal(String(a.originalOrderedQuantity)),b=e.grid.view.windowId;if(b===OB.ProductServices.rfcWindowId&&((g<0)||(h.compareTo(d.subtract(f)))>0)){e.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR,null,OB.I18N.getLabel("OBUIAPP_RM_OutOfRange",[d.subtract(f).toString()]));
return false;}if(b!==OB.ProductServices.rfcWindowId&&((d.compareTo(BigDecimal.prototype.ZERO)<0)&&(((h.compareTo(d)<0))||(g>0)))){e.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR,null,OB.I18N.getLabel("ServiceQuantityLessThanOrdered",[d]));return false;}if(b!==OB.ProductServices.rfcWindowId&&((d.compareTo(BigDecimal.prototype.ZERO)>0)&&(((h.compareTo(d)>0))||(g<0)))){e.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR,null,OB.I18N.getLabel("ServiceQuantityMoreThanOrdered",[d]));
return false;}return true;};OB.ProductServices.relateOrderLinesSelectionChanged=function(a,b){this.fireOnPause("selectionChanged"+a.id,function(){OB.ProductServices.doRelateOrderLinesSelectionChanged(a,b,this.view);},200);this.Super("selectionChanged",arguments);};OB.ProductServices.doRelateOrderLinesSelectionChanged=function(e,a,h){var d=h.theForm.getItem("totallinesamount"),b=new BigDecimal(String(h.theForm.getItem("totallinesamount").getValue()||0)),f=h.theForm.getItem("grid").canvas.viewGrid,g=h.theForm.getItem("totalserviceamount"),c=new BigDecimal(String(e.originalOrderedQuantity)),i=h.windowId;
if(i!==OB.ProductServices.rfcWindowId){if(a){f.setEditValue(f.getRecordIndex(e),"relatedQuantity",Number(c.toString()));}else{f.setEditValue(f.getRecordIndex(e),"relatedQuantity",Number("0"));}}else{if(a){f.setEditValue(f.getRecordIndex(e),"relatedQuantity",Number("0"));}else{f.setEditValue(f.getRecordIndex(e),"relatedQuantity",Number("0"));
}}OB.ProductServices.updateTotalLinesAmount(h.theForm);OB.ProductServices.updateServicePrice(h,e,a);};OB.ProductServices.updateServicePrice=function(b,a,f){var h,g=b.theForm.getItem("totalserviceamount"),c=b.theForm.getItem("grid").canvas.viewGrid,e,d;if(a){e=a.id;}else{e=null;}d=c.view.parentWindow.activeView.getContextInfo(false,true,true,true);
if(!d.inpTabId){d=c.view.parentWindow.activeView.parentView.getContextInfo(false,true,true,true);}h=function(i,k,j){if(k.amount||k.amount===0){g.setValue(Number(k.amount));if(k.message){c.view.messageBar.setMessage(isc.OBMessageBar.TYPE_WARNING,k.message.title,k.message.text);}}else{c.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR,k.message.title,k.message.text);
if(a){c.deselectRecord(a);}}};OB.RemoteCallManager.call("org.openbravo.common.actionhandler.ServiceRelatedLinePriceActionHandler",{orderlineId:b.theForm.getItem("orderlineId").getValue(),amount:b.theForm.getItem("totallinesamount").getValue(),discounts:b.theForm.getItem("totaldiscountsamount").getValue(),priceamount:b.theForm.getItem("totapriceamount").getValue(),relatedqty:b.theForm.getItem("totalrelatedqty").getValue(),unitdiscountsamt:b.theForm.getItem("totalUnitDiscountsAmt").getValue(),orderLineToRelateId:e,tabId:d.inpTabId,state:f},{},h);
};