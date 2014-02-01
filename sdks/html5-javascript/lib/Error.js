
Usergrid.Error=function(options){
	this.name="UsergridError";
	this.timestamp=options.timestamp||Date.now();
	if(options instanceof Error){
		this.exception=options.name||"";
		this.message=options.message||"An error has occurred";
	}else if("object"===typeof options){
		this.exception=options.error||"unknown_error";
		this.message=options.error_description||"An error has occurred";
	}else if("string"===typeof options){
		this.exception="unknown_error";
		this.message=options||"An error has occurred";
	}
}
Usergrid.Error.prototype=new Error();
