(function(k){k.fn.jqDrag=function(a){return n(this,a,"d");};k.fn.jqResize=function(a,b){return n(this,a,"r",b);};k.jqDnR={dnr:{},e:0,drag:function(a){if(j.k=="d"){l.css({left:j.X+a.pageX-j.pX,top:j.Y+a.pageY-j.pY});}else{l.css({width:Math.max(a.pageX-j.pX+j.W,0),height:Math.max(a.pageY-j.pY+j.H,0)});
if(M1){i.css({width:Math.max(a.pageX-M1.pX+M1.W,0),height:Math.max(a.pageY-M1.pY+M1.H,0)});}}return false;},stop:function(){k(document).unbind("mousemove",f.drag).unbind("mouseup",f.stop);}};var f=k.jqDnR,j=f.dnr,l=f.e,i,n=function(a,b,c,d){return a.each(function(){b=(b)?k(b,a):a;b.bind("mousedown",{e:a,k:c},function(g){var h=g.data,p={};
l=h.e;i=d?k(d):false;if(l.css("position")!="relative"){try{l.position(p);}catch(r){}}j={X:p.left||m("left")||0,Y:p.top||m("top")||0,W:m("width")||l[0].scrollWidth||0,H:m("height")||l[0].scrollHeight||0,pX:g.pageX,pY:g.pageY,k:h.k};if(i&&h.k!="d"){M1={X:p.left||f1("left")||0,Y:p.top||f1("top")||0,W:i[0].offsetWidth||f1("width")||0,H:i[0].offsetHeight||f1("height")||0,pX:g.pageX,pY:g.pageY,k:h.k};
}else{M1=false;}try{k("input.hasDatepicker",l[0]).datepicker("hide");}catch(e){}k(document).mousemove(k.jqDnR.drag).mouseup(k.jqDnR.stop);return false;});});},m=function(a){return parseInt(l.css(a))||false;};f1=function(a){return parseInt(i.css(a))||false;};})(jQuery);