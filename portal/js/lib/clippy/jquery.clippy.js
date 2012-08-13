/*!
	clippy-jquery: <http://github.com/jimmysawczuk/clippy-jquery>
	(c) 2011-2012; MIT License, see README.md for full license information and acknowledgements
*/
(function($)
{
	var _opts = { // default options
		'width': 14,
		'height': 14,
		'clippy_path': 'clippy.swf',
		'keep_text': false,
		'force_load' : false,
		'flashvars'  : {}
	};

	$.fn.clippy = function(opts) 
	{
		opts = $.extend(_opts, opts);
		
		var hasFlash = false;
		try
		{
			var fo = new ActiveXObject('ShockwaveFlash.ShockwaveFlash');
			if (fo)
			{
				hasFlash = true;
			}
		}
		catch(e)
		{
			if (navigator.mimeTypes ["application/x-shockwave-flash"] != undefined)
			{
				hasFlash = true;
			}
		}
		
		// if browser has Flash support or manual override set...
		if (hasFlash || opts.force_load) 
		{ 
			// for every element matched...
			$.each($(this), function(idx, val)
			{
				var text = "";
				if (typeof opts.text != "undefined")
				{
					text = opts.text;
				}
				else if ($(val).data('text') && $.trim($(val).data('text')) != '')
				{
					text = $(val).data('text');
				}
				else
				{
					text = $.trim($(val).text());
				}
				
				// text should be URI-encoded, per https://github.com/mojombo/clippy/pull/9
				text = encodeURIComponent(text);
				
				var id = "";
				if (typeof $(val).attr('id') === "undefined" || $.trim($(val).attr('id')) === "")
				{
					var id_suffix = Math.round(Math.random() * 10240).toString(16);
					id = 'clippy-' + id_suffix;
					
					$(val).attr('id', id);
				}
				else
				{
					id = $(val).attr('id');
				}
				
				if (!opts.keep_text)
				{
					$(val).html('');
				}

				var flashvars = $.extend({}, opts.flashvars, {text: text});

				swfobject.embedSWF(opts.clippy_path, id, opts.width, opts.height, 
					'10', false, flashvars, {scale: "noscale"});
			});
		}
		else
		{
			// hide all the clippies so unwanted text is not displayed when Flash is not supported
			$.each(this, function(idx, val)
			{
				$(val).css({'display': 'none'});
			});
		}
	};
})(jQuery);