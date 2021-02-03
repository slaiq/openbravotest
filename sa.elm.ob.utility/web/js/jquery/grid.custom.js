(function(b){b.jgrid.extend({getColProp:function(i){var a={},g=this[0];if(!g.grid){return false;}var h=g.p.colModel;for(var j=0;j<h.length;j++){if(h[j].name==i){a=h[j];break;}}return a;},setColProp:function(d,a){return this.each(function(){if(this.grid){if(a){var c=this.p.colModel;for(var f=0;f<c.length;
f++){if(c[f].name==d){b.extend(this.p.colModel[f],a);break;}}}}});},sortGrid:function(f,a,e){return this.each(function(){var c=this,j=-1;if(!c.grid){return;}if(!f){f=c.p.sortname;}for(var d=0;d<c.p.colModel.length;d++){if(c.p.colModel[d].index==f||c.p.colModel[d].name==f){j=d;break;}}if(j!=-1){var i=c.p.colModel[j].sortable;
if(typeof i!=="boolean"){i=true;}if(typeof a!=="boolean"){a=false;}if(i){c.sortData("jqgh_"+f,j,a,e);}}});},GridDestroy:function(){return this.each(function(){if(this.grid){if(this.p.pager){b(this.p.pager).remove();}var d=this.id;try{b("#gbox_"+d).remove();}catch(a){}}});},GridUnload:function(){return this.each(function(){if(!this.grid){return;
}var e={id:b(this).attr("id"),cl:b(this).attr("class")};if(this.p.pager){b(this.p.pager).empty().removeClass("ui-state-default ui-jqgrid-pager corner-bottom");}var a=document.createElement("table");b(a).attr({id:e.id});a.className=e.cl;var f=this.id;b(a).removeClass("ui-jqgrid-btable");if(b(this.p.pager).parents("#gbox_"+f).length===1){b(a).insertBefore("#gbox_"+f).show();
b(this.p.pager).insertBefore("#gbox_"+f);}else{b(a).insertBefore("#gbox_"+f).show();}b("#gbox_"+f).remove();});},setGridState:function(a){return this.each(function(){if(!this.grid){return;}var d=this;if(a=="hidden"){b(".ui-jqgrid-bdiv, .ui-jqgrid-hdiv","#gview_"+d.p.id).slideUp("fast");if(d.p.pager){b(d.p.pager).slideUp("fast");
}if(d.p.toppager){b(d.p.toppager).slideUp("fast");}if(d.p.toolbar[0]===true){if(d.p.toolbar[1]=="both"){b(d.grid.ubDiv).slideUp("fast");}b(d.grid.uDiv).slideUp("fast");}if(d.p.footerrow){b(".ui-jqgrid-sdiv","#gbox_"+d.p.id).slideUp("fast");}b(".ui-jqgrid-titlebar-close span",d.grid.cDiv).removeClass("ui-icon-circle-triangle-n").addClass("ui-icon-circle-triangle-s");
d.p.gridstate="hidden";}else{if(a=="visible"){b(".ui-jqgrid-hdiv, .ui-jqgrid-bdiv","#gview_"+d.p.id).slideDown("fast");if(d.p.pager){b(d.p.pager).slideDown("fast");}if(d.p.toppager){b(d.p.toppager).slideDown("fast");}if(d.p.toolbar[0]===true){if(d.p.toolbar[1]=="both"){b(d.grid.ubDiv).slideDown("fast");
}b(d.grid.uDiv).slideDown("fast");}if(d.p.footerrow){b(".ui-jqgrid-sdiv","#gbox_"+d.p.id).slideDown("fast");}b(".ui-jqgrid-titlebar-close span",d.grid.cDiv).removeClass("ui-icon-circle-triangle-s").addClass("ui-icon-circle-triangle-n");d.p.gridstate="visible";}}});},updateGridRows:function(j,l,k){var a,h=false,i;
this.each(function(){var f=this,d,c,e,g;if(!f.grid){return false;}if(!l){l="id";}if(j&&j.length>0){b(j).each(function(p){e=this;c=f.rows.namedItem(e[l]);if(c){g=e[l];if(k===true){if(f.p.jsonReader.repeatitems===true){if(f.p.jsonReader.cell){e=e[f.p.jsonReader.cell];}for(var o=0;o<e.length;o++){d=f.formatter(g,e[o],o,e,"edit");
i=f.p.colModel[o].title?{"title":b.jgrid.stripHtml(d)}:{};if(f.p.treeGrid===true&&a==f.p.ExpandColumn){b("td:eq("+o+") > span:first",c).html(d).attr(i);}else{b("td:eq("+o+")",c).html(d).attr(i);}}h=true;return true;}}b(f.p.colModel).each(function(m){a=k===true?this.jsonmap||this.name:this.name;if(e[a]!==undefined){d=f.formatter(g,e[a],m,e,"edit");
i=this.title?{"title":b.jgrid.stripHtml(d)}:{};if(f.p.treeGrid===true&&a==f.p.ExpandColumn){b("td:eq("+m+") > span:first",c).html(d).attr(i);}else{b("td:eq("+m+")",c).html(d).attr(i);}h=true;}});}});}});return h;},filterGrid:function(d,a){a=b.extend({gridModel:false,gridNames:false,gridToolbar:false,filterModel:[],formtype:"horizontal",autosearch:true,formclass:"filterform",tableclass:"filtertable",buttonclass:"filterbutton",searchButton:"Search",clearButton:"Clear",enableSearch:false,enableClear:false,beforeSearch:null,afterSearch:null,beforeClear:null,afterClear:null,url:"",marksearched:true},a||{});
return this.each(function(){var c=this;this.p=a;if(this.p.filterModel.length===0&&this.p.gridModel===false){alert("No filter is set");return;}if(!d){alert("No target grid is set!");return;}this.p.gridid=d.indexOf("#")!=-1?d:"#"+d;var t=b(this.p.gridid).jqGrid("getGridParam","colModel");if(t){if(this.p.gridModel===true){var s=b(this.p.gridid)[0];
var q;b.each(t,function(g,f){var e=[];this.search=this.search===false?false:true;if(this.editrules&&this.editrules.searchhidden===true){q=true;}else{if(this.hidden===true){q=false;}else{q=true;}}if(this.search===true&&q===true){if(c.p.gridNames===true){e.label=s.p.colNames[g];}else{e.label="";}e.name=this.name;
e.index=this.index||this.name;e.stype=this.edittype||"text";if(e.stype!="select"){e.stype="text";}e.defval=this.defval||"";e.surl=this.surl||"";e.sopt=this.editoptions||{};e.width=this.width;c.p.filterModel.push(e);}});}else{b.each(c.p.filterModel,function(g,f){for(var e=0;e<t.length;e++){if(this.name==t[e].name){this.index=t[e].index||this.name;
break;}}if(!this.index){this.index=this.name;}});}}else{alert("Could not get grid colModel");return;}var p=function(){var h={},i=0,k;var j=b(c.p.gridid)[0],e;j.p.searchdata={};if(b.isFunction(c.p.beforeSearch)){c.p.beforeSearch();}b.each(c.p.filterModel,function(x,l){e=this.index;switch(this.stype){case"select":k=b("select[name="+e+"]",c).val();
if(k){h[e]=k;if(c.p.marksearched){b("#jqgh_"+this.name,j.grid.hDiv).addClass("dirty-cell");}i++;}else{if(c.p.marksearched){b("#jqgh_"+this.name,j.grid.hDiv).removeClass("dirty-cell");}try{delete j.p.postData[this.index];}catch(w){}}break;default:k=b("input[name="+e+"]",c).val();if(k){h[e]=k;if(c.p.marksearched){b("#jqgh_"+this.name,j.grid.hDiv).addClass("dirty-cell");
}i++;}else{if(c.p.marksearched){b("#jqgh_"+this.name,j.grid.hDiv).removeClass("dirty-cell");}try{delete j.p.postData[this.index];}catch(w){}}}});var g=i>0?true:false;b.extend(j.p.postData,h);var f;if(c.p.url){f=b(j).jqGrid("getGridParam","url");b(j).jqGrid("setGridParam",{url:c.p.url});}b(j).jqGrid("setGridParam",{search:g}).trigger("reloadGrid",[{page:1}]);
if(f){b(j).jqGrid("setGridParam",{url:f});}if(b.isFunction(c.p.afterSearch)){c.p.afterSearch();}};var m=function(){var h={},k,i=0;var j=b(c.p.gridid)[0],e;if(b.isFunction(c.p.beforeClear)){c.p.beforeClear();}b.each(c.p.filterModel,function(z,l){e=this.index;k=(this.defval)?this.defval:"";if(!this.stype){this.stype="text";
}switch(this.stype){case"select":var x;b("select[name="+e+"] option",c).each(function(u){if(u===0){this.selected=true;}if(b(this).text()==k){this.selected=true;x=b(this).val();return false;}});if(x){h[e]=x;if(c.p.marksearched){b("#jqgh_"+this.name,j.grid.hDiv).addClass("dirty-cell");}i++;}else{if(c.p.marksearched){b("#jqgh_"+this.name,j.grid.hDiv).removeClass("dirty-cell");
}try{delete j.p.postData[this.index];}catch(y){}}break;case"text":b("input[name="+e+"]",c).val(k);if(k){h[e]=k;if(c.p.marksearched){b("#jqgh_"+this.name,j.grid.hDiv).addClass("dirty-cell");}i++;}else{if(c.p.marksearched){b("#jqgh_"+this.name,j.grid.hDiv).removeClass("dirty-cell");}try{delete j.p.postData[this.index];
}catch(y){}}break;}});var g=i>0?true:false;b.extend(j.p.postData,h);var f;if(c.p.url){f=b(j).jqGrid("getGridParam","url");b(j).jqGrid("setGridParam",{url:c.p.url});}b(j).jqGrid("setGridParam",{search:g}).trigger("reloadGrid",[{page:1}]);if(f){b(j).jqGrid("setGridParam",{url:f});}if(b.isFunction(c.p.afterClear)){c.p.afterClear();
}};var r;var n=function(){var h=document.createElement("tr");var j,f,e,i,g;if(c.p.formtype=="horizontal"){b(r).append(h);}b.each(c.p.filterModel,function(E,J){i=document.createElement("td");b(i).append("<label for='"+this.name+"'>"+this.label+"</label>");g=document.createElement("td");var F=this;if(!this.stype){this.stype="text";
}switch(this.stype){case"select":if(this.surl){b(g).load(this.surl,function(){if(F.defval){b("select",this).val(F.defval);}b("select",this).attr({name:F.index||F.name,id:"sg_"+F.name});if(F.sopt){b("select",this).attr(F.sopt);}if(c.p.gridToolbar===true&&F.width){b("select",this).width(F.width);}if(c.p.autosearch===true){b("select",this).change(function(u){p();
return false;});}});}else{if(F.sopt.value){var L=F.sopt.value;var I=document.createElement("select");b(I).attr({name:F.index||F.name,id:"sg_"+F.name}).attr(F.sopt);var K,k,H;if(typeof L==="string"){K=L.split(";");for(var G=0;G<K.length;G++){k=K[G].split(":");H=document.createElement("option");H.value=k[0];
H.innerHTML=k[1];if(k[1]==F.defval){H.selected="selected";}I.appendChild(H);}}else{if(typeof L==="object"){for(var l in L){if(L.hasOwnProperty(l)){E++;H=document.createElement("option");H.value=l;H.innerHTML=L[l];if(L[l]==F.defval){H.selected="selected";}I.appendChild(H);}}}}if(c.p.gridToolbar===true&&F.width){b(I).width(F.width);
}b(g).append(I);if(c.p.autosearch===true){b(I).change(function(u){p();return false;});}}}break;case"text":var D=this.defval?this.defval:"";b(g).append("<input type='text' name='"+(this.index||this.name)+"' id='sg_"+this.name+"' value='"+D+"'/>");if(F.sopt){b("input",g).attr(F.sopt);}if(c.p.gridToolbar===true&&F.width){if(b.browser.msie){b("input",g).width(F.width-4);
}else{b("input",g).width(F.width-2);}}if(c.p.autosearch===true){b("input",g).keypress(function(u){var v=u.charCode?u.charCode:u.keyCode?u.keyCode:0;if(v==13){p();return false;}return this;});}break;}if(c.p.formtype=="horizontal"){if(c.p.gridToolbar===true&&c.p.gridNames===false){b(h).append(g);}else{b(h).append(i).append(g);
}b(h).append(g);}else{j=document.createElement("tr");b(j).append(i).append(g);b(r).append(j);}});g=document.createElement("td");if(c.p.enableSearch===true){f="<input type='button' id='sButton' class='"+c.p.buttonclass+"' value='"+c.p.searchButton+"'/>";b(g).append(f);b("input#sButton",g).click(function(){p();
return false;});}if(c.p.enableClear===true){e="<input type='button' id='cButton' class='"+c.p.buttonclass+"' value='"+c.p.clearButton+"'/>";b(g).append(e);b("input#cButton",g).click(function(){m();return false;});}if(c.p.enableClear===true||c.p.enableSearch===true){if(c.p.formtype=="horizontal"){b(h).append(g);
}else{j=document.createElement("tr");b(j).append("<td>&#160;</td>").append(g);b(r).append(j);}}};var o=b("<form name='SearchForm' style=display:inline;' class='"+this.p.formclass+"'></form>");r=b("<table class='"+this.p.tableclass+"' cellspacing='0' cellpading='0' border='0'><tbody></tbody></table>");
b(o).append(r);n();b(this).append(o);this.triggerSearch=p;this.clearSearch=m;});},filterToolbar:function(a){a=b.extend({autosearch:true,searchOnEnter:true,beforeSearch:null,afterSearch:null,beforeClear:null,afterClear:null,searchurl:"",stringResult:false,groupOp:"AND",defaultSearch:"bw"},a||{});return this.each(function(){var j=this;
var p=function(){var f={},g=0,x,w,v={},i;b.each(j.p.colModel,function(s,q){w=this.index||this.name;switch(this.stype){case"select":i=(this.searchoptions&&this.searchoptions.sopt)?this.searchoptions.sopt[0]:"eq";x=b("select[name="+w+"]",j.grid.hDiv).val();if(x){f[w]=x;v[w]=i;g++;}else{try{delete j.p.postData[w];
}catch(r){}}break;case"text":i=(this.searchoptions&&this.searchoptions.sopt)?this.searchoptions.sopt[0]:a.defaultSearch;x=b("input[name="+w+"]",j.grid.hDiv).val();if(x){f[w]=x;v[w]=i;g++;}else{try{delete j.p.postData[w];}catch(r){}}break;}});var c=g>0?true:false;if(a.stringResult===true||j.p.datatype=="local"){var y='{"groupOp":"'+a.groupOp+'","rules":[';
var d=0;b.each(f,function(r,q){if(d>0){y+=",";}y+='{"field":"'+r+'",';y+='"op":"'+v[r]+'",';y+='"data":"'+q+'"}';d++;});y+="]}";b.extend(j.p.postData,{filters:y});}else{b.extend(j.p.postData,f);}var e;if(j.p.searchurl){e=j.p.url;b(j).jqGrid("setGridParam",{url:j.p.searchurl});}var h=false;if(b.isFunction(a.beforeSearch)){h=a.beforeSearch.call(j);
}if(!h){b(j).jqGrid("setGridParam",{search:c}).trigger("reloadGrid",[{page:1}]);}if(e){b(j).jqGrid("setGridParam",{url:e});}if(b.isFunction(a.afterSearch)){a.afterSearch();}};var l=function(u){var h={},w,i=0,v;u=(typeof u!="boolean")?true:u;b.each(j.p.colModel,function(t,q){w=(this.searchoptions&&this.searchoptions.defaultValue)?this.searchoptions.defaultValue:"";
v=this.index||this.name;switch(this.stype){case"select":var r;b("select[name="+v+"] option",j.grid.hDiv).each(function(y){if(y===0){this.selected=true;}if(b(this).text()==w){this.selected=true;r=b(this).val();return false;}});if(r){h[v]=r;i++;}else{try{delete j.p.postData[v];}catch(s){}}break;case"text":b("input[name="+v+"]",j.grid.hDiv).val(w);
if(w){h[v]=w;i++;}else{try{delete j.p.postData[v];}catch(s){}}break;}});var e=i>0?true:false;if(a.stringResult===true||j.p.datatype=="local"){var d='{"groupOp":"'+a.groupOp+'","rules":[';var f=0;b.each(h,function(r,q){if(f>0){d+=",";}d+='{"field":"'+r+'",';d+='"op":"'+"eq"+'",';d+='"data":"'+q+'"}';f++;
});d+="]}";b.extend(j.p.postData,{filters:d});}else{b.extend(j.p.postData,h);}var g;if(j.p.searchurl){g=j.p.url;b(j).jqGrid("setGridParam",{url:j.p.searchurl});}var c=false;if(b.isFunction(a.beforeClear)){c=a.beforeClear.call(j);}if(!c){if(u){b(j).jqGrid("setGridParam",{search:e}).trigger("reloadGrid",[{page:1}]);
}}if(g){b(j).jqGrid("setGridParam",{url:g});}if(b.isFunction(a.afterClear)){a.afterClear();}};var n=function(){var c=b("tr.ui-search-toolbar",j.grid.hDiv);if(c.css("display")=="none"){c.show();}else{c.hide();}};function k(e,c){var d=b(e);if(d[0]){jQuery.each(c,function(){if(this.data!==undefined){d.bind(this.type,this.data,this.fn);
}else{d.bind(this.type,this.fn);}});}}var m=b("<tr class='ui-search-toolbar' role='rowheader'></tr>");var o;b.each(j.p.colModel,function(H,e){var E=this,G,h,J,B,g;h=b("<th role='columnheader' class='ui-state-default ui-th-column ui-th-"+j.p.direction+"'></th>");G=b("<div style='width:100%;position:relative;height:100%;padding-right:0.3em;'></div>");
if(this.hidden===true){b(h).css("display","none");}this.search=this.search===false?false:true;if(typeof this.stype=="undefined"){this.stype="text";}J=b.extend({},this.searchoptions||{});if(this.search){switch(this.stype){case"select":B=this.surl||J.dataUrl;if(B){g=G;b.ajax(b.extend({url:B,dataType:"html",complete:function(s,q){if(J.buildSelect!==undefined){var r=J.buildSelect(s);
if(r){b(g).append(r);}}else{b(g).append(s.responseText);}if(J.defaultValue){b("select",g).val(J.defaultValue);}b("select",g).attr({name:E.index||E.name,id:"gs_"+E.name});if(J.attr){b("select",g).attr(J.attr);}b("select",g).css({width:"100%"});if(J.dataInit!==undefined){J.dataInit(b("select",g)[0]);}if(J.dataEvents!==undefined){k(b("select",g)[0],J.dataEvents);
}if(a.autosearch===true){b("select",g).change(function(t){p();return false;});}s=null;}},b.jgrid.ajaxOptions,j.p.ajaxSelectOptions||{}));}else{var i;if(E.searchoptions&&E.searchoptions.value){i=E.searchoptions.value;}else{if(E.editoptions&&E.editoptions.value){i=E.editoptions.value;}}if(i){var c=document.createElement("select");
c.style.width="100%";b(c).attr({name:E.index||E.name,id:"gs_"+E.name});var f,C,d;if(typeof i==="string"){f=i.split(";");for(var I=0;I<f.length;I++){C=f[I].split(":");d=document.createElement("option");d.value=C[0];d.innerHTML=C[1];c.appendChild(d);}}else{if(typeof i==="object"){for(var D in i){if(i.hasOwnProperty(D)){d=document.createElement("option");
d.value=D;d.innerHTML=i[D];c.appendChild(d);}}}}if(J.defaultValue){b(c).val(J.defaultValue);}if(J.attr){b(c).attr(J.attr);}if(J.dataInit!==undefined){J.dataInit(c);}if(J.dataEvents!==undefined){k(c,J.dataEvents);}b(G).append(c);if(a.autosearch===true){b(c).change(function(q){p();return false;});}}}break;
case"text":var F=J.defaultValue?J.defaultValue:"";b(G).append("<input type='text' style='width:95%;padding:0px;' name='"+(E.index||E.name)+"' id='gs_"+E.name+"' value='"+F+"'/>");if(J.attr){b("input",G).attr(J.attr);}if(J.dataInit!==undefined){J.dataInit(b("input",G)[0]);}if(J.dataEvents!==undefined){k(b("input",G)[0],J.dataEvents);
}if(a.autosearch===true){if(a.searchOnEnter){b("input",G).keypress(function(r){var q=r.charCode?r.charCode:r.keyCode?r.keyCode:0;if(q==13){p();return false;}return this;});}else{b("input",G).keydown(function(r){var q=r.which;switch(q){case 13:return false;case 9:case 16:case 37:case 38:case 39:case 40:case 27:break;
default:if(o){clearTimeout(o);}o=setTimeout(function(){p();},500);}});}}break;}}b(h).append(G);b(m).append(h);});b("table thead",j.grid.hDiv).append(m);this.triggerToolbar=p;this.clearToolbar=l;this.toggleToolbar=n;});}});})(jQuery);