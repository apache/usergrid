/**
 * @author tPeregrina
 */
(function($){
	
	$.fn.attachClippy = function(options) {
		
		$.fn.attachClippy.defaults = {
			clippyContainerId: $("#global-clippy-container"),
			clippyDataId: $("#global-clippy-data"),
			dataId: "",
		};
		
		options = $.extend({}, $.fn.attachClippy.defaults, options);
			
		return this.each(function(){
			
			$(this).mouseover(function(){
				var offset = $(this).offset();
				options.clippyContainerId.css({
					//TODO: 1 is the offset given by the HTML tag border-left of 1, find a better way to check and apply this offset.
					left: Math.round(offset.left)-1,
					top: Math.round(offset.top),
				});
				//Set Data to be copied by clippy
				options.clippyDataId.text(options.dataId.text());
			});
			//hide clippy movie
			options.clippyContainerId.mouseout(function(){
				
				options.clippyContainerId.css({
					left: -9999,
					top: -9999,
				});
			});	
		});
	};
	
	$.fn.detachClippy = function(){
		return this.each(function(){
			$(this).unbind('mouseover', 'mouseout');
		});		
	};
	
})(jQuery);
