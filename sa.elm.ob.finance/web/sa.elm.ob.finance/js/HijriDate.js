isc.defineClass('EFIN_HijriDateNow', isc.Label);
 
isc.EFIN_HijriDateNow.addProperties({
  height: 1,
  width: 100,
  overflow: 'visible',
  contents: '',
  initWidget: function() {
	  if (this.record) 	{	 
		  this.computeContents(this.record['paymentDate']);
  }
	  else if (this.canvasItem) {
      this.computeContents(this.canvasItem.form.getValue('paymentDate'));
    }
 
    this.Super('initWidget', arguments);
  },
  
  // is called when the form gets redrawn
  redrawingItem: function() {
	  if (this.record) {		  
		  this.computeContents(this.record['paymentDate']);
	  }else if (this.canvasItem) {
    	this.computeContents(this.canvasItem.form.getValue('paymentDate'));
    }
  },
  
  // is called when a field on the form changes its value
  onItemChanged: function() {
	  if (this.record) {		  
		  this.computeContents(this.record['paymentDate']);
	  }else if (this.canvasItem) {
    	this.computeContents(this.canvasItem.form.getValue('paymentDate'));
    }
  },
  
  // is called in grid-display mode when the canvas is created/used
  // for a record
  setRecord: function(record) {
	  if (this.record) {		  
		  this.computeContents(this.record['paymentDate']);
	  }else if (this.canvasItem) {
    	this.computeContents(this.canvasItem.form.getValue('paymentDate'));
    }
  },
  
  computeContents: function(date) {
	  if(date=='')
		  this.setContents('');
	  else
		  {
		  	var itemVal= this;
		  	var callback = function(response,data,request){
		  		itemVal.setContents(data.hijridate);
		  	}
		    OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.EFIN_PaymentGrego_To_Hijri', 
					{paymentdate: date}, {}, callback);	
		  }
   
  }
});
