jQuery.fn.searchFilter=function(e,f){function d(L,F,O){this.$=L;this.add=function(g){if(g==null){L.find(".ui-add-last").click();}else{L.find(".sf:eq("+g+") .ui-add").click();}return this;};this.del=function(g){if(g==null){L.find(".sf:last .ui-del").click();}else{L.find(".sf:eq("+g+") .ui-del").click();
}return this;};this.search=function(g){L.find(".ui-search").click();return this;};this.reset=function(g){if(g===undefined){g=false;}L.find(".ui-reset").trigger("click",[g]);return this;};this.close=function(){L.find(".ui-closer").click();return this;};if(F!=null){function z(){jQuery(this).toggleClass("ui-state-hover");
return false;}function M(g){jQuery(this).toggleClass("ui-state-active",(g.type=="mousedown"));return false;}function Q(h,g){return"<option value='"+h+"'>"+g+"</option>";}function C(i,h,g){return"<select class='"+i+"'"+(g?" style='display:none;'":"")+">"+h+"</select>";}function c(h,i){var g=L.find("tr.sf td.data "+h);
if(g[0]!=null){i(g);}}function E(h,i){var g=L.find("tr.sf td.data "+h);if(g[0]!=null){jQuery.each(i,function(){if(this.data!=null){g.bind(this.type,this.data,this.fn);}else{g.bind(this.type,this.fn);}});}}var H=jQuery.extend({},jQuery.fn.searchFilter.defaults,O);var a=-1;var b="";jQuery.each(H.groupOps,function(){b+=Q(this.op,this.text);
});b="<select name='groupOp'>"+b+"</select>";L.html("").addClass("ui-searchFilter").append("<div class='ui-widget-overlay' style='z-index: -1'>&#160;</div><table class='ui-widget-content ui-corner-all'><thead><tr><td colspan='5' class='ui-widget-header ui-corner-all' style='line-height: 18px;'><div class='ui-closer ui-state-default ui-corner-all ui-helper-clearfix' style='float: right;'><span class='ui-icon ui-icon-close'></span></div>"+H.windowTitle+"</td></tr></thead><tbody><tr class='sf'><td class='fields'></td><td class='ops'></td><td class='data'></td><td><div class='ui-del ui-state-default ui-corner-all'><span class='ui-icon ui-icon-minus'></span></div></td><td><div class='ui-add ui-state-default ui-corner-all'><span class='ui-icon ui-icon-plus'></span></div></td></tr><tr><td colspan='5' class='divider'><hr class='ui-widget-content' style='margin:1px'/></td></tr></tbody><tfoot><tr><td colspan='3'><span class='ui-reset ui-state-default ui-corner-all' style='display: inline-block; float: left;'><span class='ui-icon ui-icon-arrowreturnthick-1-w' style='float: left;'></span><span style='line-height: 18px; padding: 0 7px 0 3px;'>"+H.resetText+"</span></span><span class='ui-search ui-state-default ui-corner-all' style='display: inline-block; float: right;'><span class='ui-icon ui-icon-search' style='float: left;'></span><span style='line-height: 18px; padding: 0 7px 0 3px;'>"+H.searchText+"</span></span><span class='matchText'>"+H.matchText+"</span> "+b+" <span class='rulesText'>"+H.rulesText+"</span></td><td>&#160;</td><td><div class='ui-add-last ui-state-default ui-corner-all'><span class='ui-icon ui-icon-plusthick'></span></div></td></tr></tfoot></table>");
var K=L.find("tr.sf");var N=K.find("td.fields");var P=K.find("td.ops");var G=K.find("td.data");var D="";jQuery.each(H.operators,function(){D+=Q(this.op,this.text);});D=C("default",D,true);P.append(D);var J="<input type='text' class='default' style='display:none;' />";G.append(J);var A="";var B=false;
var R=false;jQuery.each(F,function(l){var m=l;A+=Q(this.itemval,this.text);if(this.ops!=null){B=true;var k="";jQuery.each(this.ops,function(){k+=Q(this.op,this.text);});k=C("field"+m,k,true);P.append(k);}if(this.dataUrl!=null){if(l>a){a=l;}R=true;var h=this.dataEvents;var j=this.dataInit;var g=this.buildSelect;
jQuery.ajax(jQuery.extend({url:this.dataUrl,complete:function(n){var o;if(g!=null){o=jQuery("<div />").append(g(n));}else{o=jQuery("<div />").append(n.responseText);}o.find("select").addClass("field"+m).hide();G.append(o.html());if(j){c(".field"+l,j);}if(h){E(".field"+l,h);}if(l==a){L.find("tr.sf td.fields select[name='field']").change();
}}},H.ajaxSelectOptions));}else{if(this.dataValues!=null){R=true;var i="";jQuery.each(this.dataValues,function(){i+=Q(this.value,this.text);});i=C("field"+m,i,true);G.append(i);}else{if(this.dataEvents!=null||this.dataInit!=null){R=true;var i="<input type='text' class='field"+m+"' />";G.append(i);}}}if(this.dataInit!=null&&l!=a){c(".field"+l,this.dataInit);
}if(this.dataEvents!=null&&l!=a){E(".field"+l,this.dataEvents);}});A="<select name='field'>"+A+"</select>";N.append(A);var I=N.find("select[name='field']");if(B){I.change(function(j){var g=j.target.selectedIndex;var i=jQuery(j.target).parents("tr.sf").find("td.ops");i.find("select").removeAttr("name").hide();
var h=i.find(".field"+g);if(h[0]==null){h=i.find(".default");}h.attr("name","op").show();return false;});}else{P.find(".default").attr("name","op").show();}if(R){I.change(function(j){var g=j.target.selectedIndex;var i=jQuery(j.target).parents("tr.sf").find("td.data");i.find("select,input").removeClass("vdata").hide();
var h=i.find(".field"+g);if(h[0]==null){h=i.find(".default");}h.show().addClass("vdata");return false;});}else{G.find(".default").show().addClass("vdata");}if(B||R){I.change();}L.find(".ui-state-default").hover(z,z).mousedown(M).mouseup(M);L.find(".ui-closer").click(function(g){H.onClose(jQuery(L.selector));
return false;});L.find(".ui-del").click(function(h){var g=jQuery(h.target).parents(".sf");if(g.siblings(".sf").length>0){if(H.datepickerFix===true&&jQuery.fn.datepicker!==undefined){g.find(".hasDatepicker").datepicker("destroy");}g.remove();}else{g.find("select[name='field']")[0].selectedIndex=0;g.find("select[name='op']")[0].selectedIndex=0;
g.find(".data input").val("");g.find(".data select").each(function(){this.selectedIndex=0;});g.find("select[name='field']").change(function(i){i.stopPropagation();});}return false;});L.find(".ui-add").click(function(j){var h=jQuery(j.target).parents(".sf");var k=h.clone(true).insertAfter(h);k.find(".ui-state-default").removeClass("ui-state-hover ui-state-active");
if(H.clone){k.find("select[name='field']")[0].selectedIndex=h.find("select[name='field']")[0].selectedIndex;var g=(k.find("select[name='op']")[0]==null);if(!g){k.find("select[name='op']").focus()[0].selectedIndex=h.find("select[name='op']")[0].selectedIndex;}var i=k.find("select.vdata");if(i[0]!=null){i[0].selectedIndex=h.find("select.vdata")[0].selectedIndex;
}}else{k.find(".data input").val("");k.find("select[name='field']").focus();}if(H.datepickerFix===true&&jQuery.fn.datepicker!==undefined){h.find(".hasDatepicker").each(function(){var l=jQuery.data(this,"datepicker").settings;k.find("#"+this.id).unbind().removeAttr("id").removeClass("hasDatepicker").datepicker(l);
});}k.find("select[name='field']").change(function(l){l.stopPropagation();});return false;});L.find(".ui-search").click(function(i){var j=jQuery(L.selector);var h;var g=j.find("select[name='groupOp'] :selected").val();if(!H.stringResult){h={groupOp:g,rules:[]};}else{h='{"groupOp":"'+g+'","rules":[';}j.find(".sf").each(function(n){var k=jQuery(this).find("select[name='field'] :selected").val();
var l=jQuery(this).find("select[name='op'] :selected").val();var m=jQuery(this).find("input.vdata,select.vdata :selected").val();m+="";m=m.replace(/\\/g,"\\\\").replace(/\"/g,'\\"');if(!H.stringResult){h.rules.push({field:k,op:l,data:m});}else{if(n>0){h+=",";}h+='{"field":"'+k+'",';h+='"op":"'+l+'",';
h+='"data":"'+m+'"}';}});if(H.stringResult){h+="]}";}H.onSearch(h);return false;});L.find(".ui-reset").click(function(g,i){var h=jQuery(L.selector);h.find(".ui-del").click();h.find("select[name='groupOp']")[0].selectedIndex=0;H.onReset(i);return false;});L.find(".ui-add-last").click(function(){var g=jQuery(L.selector+" .sf:last");
var h=g.clone(true).insertAfter(g);h.find(".ui-state-default").removeClass("ui-state-hover ui-state-active");h.find(".data input").val("");h.find("select[name='field']").focus();if(H.datepickerFix===true&&jQuery.fn.datepicker!==undefined){g.find(".hasDatepicker").each(function(){var i=jQuery.data(this,"datepicker").settings;
h.find("#"+this.id).unbind().removeAttr("id").removeClass("hasDatepicker").datepicker(i);});}h.find("select[name='field']").change(function(i){i.stopPropagation();});return false;});this.setGroupOp=function(j){selDOMobj=L.find("select[name='groupOp']")[0];var i={},h=selDOMobj.options.length,g;for(g=0;
g<h;g++){i[selDOMobj.options[g].value]=g;}selDOMobj.selectedIndex=i[j];jQuery(selDOMobj).change(function(k){k.stopPropagation();});};this.setFilter=function(t){var g=t["sfref"],i=t["filter"];var o=[],p,q,s,n,l,m={};selDOMobj=g.find("select[name='field']")[0];for(p=0,s=selDOMobj.options.length;p<s;p++){m[selDOMobj.options[p].value]={"index":p,"ops":{}};
o.push(selDOMobj.options[p].value);}for(p=0,l=o.length;p<l;p++){selDOMobj=g.find(".ops > select[class='field"+p+"']")[0];if(selDOMobj){for(q=0,n=selDOMobj.options.length;q<n;q++){m[o[p]]["ops"][selDOMobj.options[q].value]=q;}}selDOMobj=g.find(".data > select[class='field"+p+"']")[0];if(selDOMobj){m[o[p]]["data"]={};
for(q=0,n=selDOMobj.options.length;q<n;q++){m[o[p]]["data"][selDOMobj.options[q].value]=q;}}}var r,j,h,u,k;r=i["field"];if(m[r]){j=m[r]["index"];}if(j!=null){h=m[r]["ops"][i["op"]];if(h===undefined){for(p=0,l=O.operators.length;p<l;p++){if(O.operators[p].op==i.op){h=p;break;}}}u=i["data"];if(m[r]["data"]==null){k=-1;
}else{k=m[r]["data"][u];}}if(j!=null&&h!=null&&k!=null){g.find("select[name='field']")[0].selectedIndex=j;g.find("select[name='field']").change();g.find("select[name='op']")[0].selectedIndex=h;g.find("input.vdata").val(u);g=g.find("select.vdata")[0];if(g){g.selectedIndex=k;}return true;}else{return false;
}};}}return new d(this,e,f);};jQuery.fn.searchFilter.version="1.2.9";jQuery.fn.searchFilter.defaults={clone:true,datepickerFix:true,onReset:function(b){alert("Reset Clicked. Data Returned: "+b);},onSearch:function(b){alert("Search Clicked. Data Returned: "+b);},onClose:function(b){b.hide();},groupOps:[{op:"AND",text:"all"},{op:"OR",text:"any"}],operators:[{op:"eq",text:"is equal to"},{op:"ne",text:"is not equal to"},{op:"lt",text:"is less than"},{op:"le",text:"is less or equal to"},{op:"gt",text:"is greater than"},{op:"ge",text:"is greater or equal to"},{op:"in",text:"is in"},{op:"ni",text:"is not in"},{op:"bw",text:"begins with"},{op:"bn",text:"does not begin with"},{op:"ew",text:"ends with"},{op:"en",text:"does not end with"},{op:"cn",text:"contains"},{op:"nc",text:"does not contain"}],matchText:"match",rulesText:"rules",resetText:"Reset",searchText:"Search",stringResult:true,windowTitle:"Search Rules",ajaxSelectOptions:{}};