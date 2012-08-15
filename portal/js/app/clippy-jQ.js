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
					left: Math.round(offset.left),
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
