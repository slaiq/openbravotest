(function($) {
	$('.OBToolTip').live('mouseover', function() {
		$(this).removeData("OB_ToolTip");
		$('div.OB_ToolTip').remove();
		new $.OBToolTip($(this));
	});
	var css = document.createElement("link");
	css.setAttribute("rel", "stylesheet");
	css.setAttribute("type", "text/css");
	css.setAttribute("href", "../web/js/common/OBToolTip.css");
	document.getElementsByTagName('head')[0].appendChild(css);
	$.OBToolTip = function(elements, options) {
		var defaults = {
			animation_speed : 100,
			animation_offset : 20,
			background_color : '#FFF',
			close_on_click : false,
			color : '#000',
			content : false,
			default_position : 'above',
			hide_delay : 100,
			keep_visible : true,
			max_width : 300,
			opacity : '1',
			position : 'center',
			prerender : false,
			show_delay : 100,
			vertical_offset : 0,
			onBeforeHide : null,
			onHide : null,
			onBeforeShow : function(cc) {
				setTimeout(function() {
					$('.OB_ToolTip_Text').scrollTop(0);
					$('.OB_ToolTip .OB_ToolTip_Arrow').each(function() {
						if ($(this).position().left < 0) {
							$(this).css('left', '288');
						}
					});
				}, 10);
			},
			onShow : null
		}, plugin = this, window_width, window_height, horizontal_scroll, vertical_scroll;
		plugin.settings = {};
		plugin.hide = function(elements, destroy) {
			elements.each(function() {
				var $element = $(this), tooltip_info = $element.data('OB_ToolTip');
				if (tooltip_info) {
					tooltip_info.sticky = false;
					if (destroy)
						tooltip_info.destroy = true;
					$element.data('OB_ToolTip', tooltip_info);
					_hide($element);
				}
			});
		};
		plugin.show = function(elements, destroy) {
			elements.each(function() {
				var $element = $(this), tooltip_info = $element.data('OB_ToolTip');
				if (tooltip_info) {
					tooltip_info.sticky = true;
					tooltip_info.muted = false;
					if (destroy)
						tooltip_info.destroy = true;
					$element.data('OB_ToolTip', tooltip_info);
					_show($element);
				}
			});
		};
		var _init = function() {
			plugin.settings = $.extend({}, defaults, options);
			elements.each(function() {
				var $element = $(this), title = $element.next().html(), data_attribute = $element.data('OB_ToolTip');
				if (title == '')
					title = $element.attr('OBToolTip') ? $element.attr('OBToolTip') : '';
				else
					$element.attr('OBToolTip', title);
				if ((title && title !== '') || (data_attribute && data_attribute !== '') || undefined !== plugin.settings.content) {
					$element.bind({
						'mouseenter' : function() {
							_show($element);
						},
						'mouseleave' : function() {
							_hide($element);
						}
					});
					$element.data('OB_ToolTip', {
						'tooltip' : null,
						'content' : data_attribute || title || '',
						'window_resized' : true,
						'window_scrolled' : true,
						'show_timeout' : null,
						'hide_timeout' : null,
						'animation_offset' : plugin.settings.animation_offset,
						'sticky' : false,
						'destroy' : false,
						'muted' : false
					});
					$element.attr('title', '');
					if (plugin.settings.prerender)
						_create_tooltip($element);
					_show($element);
				}
			});
			$(window).bind('scroll resize', function(event) {
				elements.each(function() {
					var tooltip_info = $(this).data('OB_ToolTip');
					if (tooltip_info) {
						if (event.type == 'scroll')
							tooltip_info.window_scrolled = true;
						else
							tooltip_info.window_resized = true;
						$(this).data('OB_ToolTip', tooltip_info);
					}
				});
			});
		};
		var _create_tooltip = function($element) {
			var tooltip_info = $element.data('OB_ToolTip');
			if (!tooltip_info.tooltip) {
				var tooltip = jQuery('<div>', {
					'class' : 'OB_ToolTip',
					css : {
						'opacity' : 0,
						'display' : 'block'
					}
				}), message = jQuery('<div>', {
					'class' : 'OB_ToolTip_Message',
					css : {
						'max-width' : plugin.settings.max_width,
						'min-width' : plugin.settings.max_width,
						'background-color' : plugin.settings.background_color,
						"overflow" : "hidden",
						'color' : plugin.settings.color
					}
				}).appendTo(tooltip), msgtext = jQuery("<div>", {
					"class" : "OB_ToolTip_Text"
				}).html(plugin.settings.content ? plugin.settings.content : tooltip_info.content).appendTo(message), arrow_container = jQuery('<div>', {
					'class' : 'OB_ToolTip_Arrow'
				}).appendTo(tooltip), arrow = jQuery('<div>').appendTo(arrow_container);
				if (plugin.settings.keep_visible) {
					tooltip.bind('mouseleave' + (plugin.settings.close_on_click ? ' click' : ''), function() {
						_hide($element);
					});
					tooltip.bind('mouseenter', function() {
						_show($element);
					});
				}
				tooltip.appendTo('body');
				var tooltip_width = tooltip.outerWidth(), tooltip_height = tooltip.outerHeight(), arrow_width = arrow.outerWidth(), arrow_height = arrow.outerHeight(), tmp_width = message.outerWidth(), tmp_height = message.outerHeight();
				tooltip_info = {
					'tooltip' : tooltip,
					'tooltip_width' : tooltip_width,
					'tooltip_height' : tooltip_height + (arrow_height / 2),
					'message' : message,
					'arrow_container' : arrow_container,
					'arrow_width' : arrow_width,
					'arrow_height' : arrow_height,
					'arrow' : arrow
				};
				tooltip.css({
					'width' : tooltip_info.tooltip_width,
					'height' : tooltip_info.tooltip_height
				});
				tooltip_info.tooltip_width = tooltip_info.tooltip_width + (message.outerWidth() - tmp_width);
				tooltip_info.tooltip_height = tooltip_info.tooltip_height + (message.outerHeight() - tmp_height);
				tooltip.css({
					'width' : tooltip_info.tooltip_width,
					'height' : tooltip_info.tooltip_height,
					'display' : 'none'
				});
				tooltip_info = $.extend($element.data('OB_ToolTip'), tooltip_info);
				$element.data('OB_ToolTip', tooltip_info);
			}
			tooltip_info.sticky = true;
			if (tooltip_info.sticky && !tooltip_info.close) {
				jQuery('<a>', {
					'class' : 'OB_ToolTip_Close',
					'href' : 'javascript:void(0)'
				}).html('x').bind('click', function(e) {
					e.preventDefault();
					var tooltip_info = $element.data('OB_ToolTip');
					tooltip_info.sticky = false;
					$element.data('OB_ToolTip', tooltip_info);
					_hide($element);
				}).appendTo(tooltip_info.message);
				tooltip_info.close = true;
				tooltip_info = $.extend($element.data('OB_ToolTip'), tooltip_info);
				$element.data('OB_ToolTip', tooltip_info);
			}
			if (tooltip_info.window_resized || tooltip_info.window_scrolled) {
				var browser_window = $(window);
				if (tooltip_info.window_resized) {
					window_width = browser_window.width();
					window_height = browser_window.height();
					var element_position = $element.offset();
					$.extend(tooltip_info, {
						'element_left' : element_position.left,
						'element_top' : element_position.top,
						'element_width' : $element.outerWidth(),
						'element_height' : $element.outerHeight()
					});
				}
				vertical_scroll = browser_window.scrollTop();
				horizontal_scroll = browser_window.scrollLeft();
				var tooltip_left = plugin.settings.position == 'left' ? tooltip_info.element_left - tooltip_info.tooltip_width + tooltip_info.arrow_width : (plugin.settings.position == 'right' ? tooltip_info.element_left + tooltip_info.element_width - tooltip_info.arrow_width
						: (tooltip_info.element_left + (tooltip_info.element_width - tooltip_info.tooltip_width) / 2)), tooltip_top = tooltip_info.element_top - tooltip_info.tooltip_height, arrow_left = plugin.settings.position == 'left' ? tooltip_info.tooltip_width - tooltip_info.arrow_width
						- (tooltip_info.arrow_width / 2) : (plugin.settings.position == 'right' ? (tooltip_info.arrow_width / 2) : ((tooltip_info.tooltip_width - tooltip_info.arrow_width) / 2));
				if (tooltip_left + tooltip_info.tooltip_width > window_width + horizontal_scroll) {
					arrow_left -= (window_width + horizontal_scroll) - (tooltip_left + tooltip_info.tooltip_width) - 6;
					tooltip_left = (window_width + horizontal_scroll) - tooltip_info.tooltip_width - 6;
					if (arrow_left + tooltip_info.arrow_width > tooltip_info.tooltip_width - 6)
						arrow_left = tooltip_info.tooltip_width - 6 - tooltip_info.arrow_width;
					if (tooltip_left + arrow_left + (tooltip_info.arrow_width / 2) < tooltip_info.element_left)
						arrow_left = -10000;
				}
				if (tooltip_left < horizontal_scroll) {
					arrow_left -= horizontal_scroll - tooltip_left;
					tooltip_left = horizontal_scroll + 2;
					if (arrow_left < 0)
						arrow_left = (tooltip_info.arrow_width / 2);
					if (tooltip_left + arrow_left + (tooltip_info.arrow_width / 2) > tooltip_info.element_left + tooltip_info.element_width)
						arrow_left = -10000;
				}
				tooltip_info.arrow_container.removeClass('OB_ToolTip_Arrow_Top');
				tooltip_info.arrow_container.addClass('OB_ToolTip_Arrow_Bottom');
				tooltip_info.message.css('margin-top', '');
				tooltip_info.arrow.css('borderColor', plugin.settings.background_color + ' transparent transparent');
				if (tooltip_top < vertical_scroll || (plugin.settings.default_position == 'below' && tooltip_info.element_top + tooltip_info.element_height + plugin.settings.vertical_offset + tooltip_info.tooltip_height + tooltip_info.animation_offset < window_height + vertical_scroll)) {
					tooltip_top = tooltip_info.element_top + tooltip_info.element_height - plugin.settings.vertical_offset;
					tooltip_info.animation_offset = Math.abs(tooltip_info.animation_offset);
					tooltip_info.message.css('margin-top', (tooltip_info.arrow_height / 2));
					tooltip_info.arrow_container.removeClass('OB_ToolTip_Arrow_Bottom');
					tooltip_info.arrow_container.addClass('OB_ToolTip_Arrow_Top');
					tooltip_info.arrow.css('borderColor', 'transparent transparent ' + plugin.settings.background_color);
				}
				else {
					tooltip_info.animation_offset = -Math.abs(tooltip_info.animation_offset);
					tooltip_top += plugin.settings.vertical_offset;
				}
				tooltip_info.arrow_container.css('left', arrow_left);
				tooltip_info.tooltip.css({
					'left' : tooltip_left,
					'top' : tooltip_top
				});
				$.extend(tooltip_info, {
					'tooltip_left' : tooltip_left,
					'tooltip_top' : tooltip_top,
					'arrow_left' : arrow_left
				});
				tooltip_info.window_resized = false;
				tooltip_info.window_scrolled = false;
				tooltip_info = $.extend($element.data('OB_ToolTip'), tooltip_info);
				$element.data('OB_ToolTip', tooltip_info);
			}
			return tooltip_info;
		};
		var _hide = function($element) {
			var tooltip_info = $element.data('OB_ToolTip');
			clearTimeout(tooltip_info.hide_timeout);
			tooltip_info.sticky = false;
			if (!tooltip_info.sticky) {
				clearTimeout(tooltip_info.show_timeout);
				tooltip_info.hide_timeout = setTimeout(function() {
					if (tooltip_info.tooltip) {
						if (plugin.settings.onBeforeHide && typeof plugin.settings.onBeforeHide == 'function')
							plugin.settings.onBeforeHide($element, tooltip_info.tooltip);
						tooltip_info.close = false;
						if (tooltip_info.destroy)
							tooltip_info.muted = true;
						$element.data('OB_ToolTip', tooltip_info);
						$('a.OB_ToolTip_Close', tooltip_info.tooltip).remove();
						tooltip_info.tooltip.stop();
						tooltip_info.tooltip.animate({
							'opacity' : 0,
							'top' : tooltip_info.tooltip_top + tooltip_info.animation_offset
						}, plugin.settings.animation_speed, function() {
							$(this).css('display', 'none');
							if (plugin.settings.onHide && typeof plugin.settings.onHide == 'function')
								plugin.settings.onHide($element, tooltip_info.tooltip);
						});
					}
				}, plugin.settings.hide_delay);
			}
		};
		var _show = function($element) {
			var tooltip_info = $element.data('OB_ToolTip');
			clearTimeout(tooltip_info.show_timeout);
			if (!tooltip_info.muted) {
				clearTimeout(tooltip_info.hide_timeout);
				tooltip_info.show_timeout = setTimeout(function() {
					tooltip_info = _create_tooltip($element);
					if (plugin.settings.onBeforeShow && typeof plugin.settings.onBeforeShow == 'function')
						plugin.settings.onBeforeShow($element, tooltip_info.tooltip);
					if (tooltip_info.tooltip.css('display') != 'block')
						tooltip_info.tooltip.css({
							'top' : tooltip_info.tooltip_top + tooltip_info.animation_offset
						});
					tooltip_info.tooltip.css('display', 'block');
					tooltip_info.tooltip.stop();
					tooltip_info.tooltip.animate({
						'top' : tooltip_info.tooltip_top,
						'opacity' : plugin.settings.opacity
					}, plugin.settings.animation_speed, function() {
						if (plugin.settings.onShow && typeof plugin.settings.onShow == 'function')
							plugin.settings.onShow($element, tooltip_info.tooltip);
					});
				}, plugin.settings.show_delay);
			}
		};
		_init();
	};
})(jQuery);
// Change Should be made in UtilityFn also
function createTooltipEle(val, len) {
	if (val == null || '' == val)
		return '';
	if (val.length <= len)
		return '<span>' + val + '</span>';
	else {
		var html = '<span style="color: inherit; font-size: inherit;">';
		html += val.substring(0, len);
		html += '<span class="OBToolTip">...</span>';
		html += '<span class="OBToolTipTxt" style="display: none;">' + val + '</span>';
		html += '</span>';
		return html;
	}
}