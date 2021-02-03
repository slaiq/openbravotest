(function() {
	var buttonProps = {
		action : function() {
			var form = document.getElementById('downloadprtemplateform');
			var iframe = document.getElementById('downloadprtemplateform');
			if (!form) {
				form = document.createElement('form');
				form.name = 'downloadprtemplateform';
				form.id = 'downloadprtemplateform';
				document.body.appendChild(form);
			}

			if (!iframe) {
				var iframe = document.createElement('iframe');
				iframe.name = "downloadprtemplateframe";
				iframe.id = "downloadprtemplateframe";
				iframe.style = "display: none";
				document.body.appendChild(iframe);
			}

			form.setAttribute("action", 
					OB.Application.contextUrl + "sa.elm.ob.scm.ad_reports.downloadprtemplate/DownloadPRTemplate?act=DownloadTemplate");
			form.setAttribute("method", "post");
			form.setAttribute("target", "downloadprtemplateframe");
			form.removeAttribute("encoding");
			form.removeAttribute("enctype");
			form.submit();
		},
		buttonType : 'escm_req_download_temp',
		prompt : OB.I18N.getLabel('ESCM_DownloadReqTemplate'),
		updateState : function() {

		}
	};
	OB.ToolbarRegistry.registerButton(buttonProps.buttonType,
			isc.OBToolbarIconButton, buttonProps, 400, '800249');
}());
