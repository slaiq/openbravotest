function StringTokenizer(a,b){this.material=a;this.separator=b;this.getTokens=getTokens;this.nextToken=nextToken;this.countTokens=countTokens;this.hasMoreTokens=hasMoreTokens;this.tokensReturned=tokensReturned;this.tokens=this.getTokens();this.tokensReturned=0;}function getTokens(){var d=new Array();
var b;if(this.material.indexOf(this.separator)<0){d[0]=this.material;return d;}start=0;end=this.material.indexOf(this.separator,start);var a=0;var c;while(this.material.length-start>=1){b=this.material.substring(start,end);start=end+1;if(this.material.indexOf(this.separator,start+1)<0){end=this.material.length;
}else{end=this.material.indexOf(this.separator,start+1);}c=trim(b);while(c.substring(0,this.separator.length)==this.separator){c=c.substring(this.separator.length);}c=trim(c);if(c==""){continue;}d[a]=c;a++;}return d;}function countTokens(){return this.tokens.length;}function nextToken(){if(this.tokensReturned>=this.tokens.length){return null;
}else{var a=this.tokens[this.tokensReturned];this.tokensReturned++;return a;}}function hasMoreTokens(){if(this.tokensReturned<this.tokens.length){return true;}else{return false;}}function tokensReturned(){return this.tokensReturned;}function trim(a){return(a.replace(/^\s+|\s+$/g,""));}