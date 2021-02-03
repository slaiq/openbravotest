
var RecordGrid = jQuery("#RecordList");
var searchFlag = 0, onSearch = 0, onDelete = 0, gridW, gridH,gGridIdList,gGridIdListCount=0;
document.getElementById("inpWindowId").value=inpWindowId;
jQuery(function() {
	RecordGrid.jqGrid({
    	direction :direction,
        url : contextPath+'/ApprovalRevokeAjax?action=GetRevokeRecords',
        colNames : [ Organization, SpecNo, Requester,NextApprover,LastActionPerformer,Status],
             colModel : [
                     {
                         name : 'org',
                         index : 'org',
                         width : 60
                     },
                     {
                         name : 'docno',
                         index : 'docno',
                         width : 60
                     },
                     {
                         name : 'requester',
                         index : 'requester',
                         width : 80
                     },
                     {
                         name : 'nextrole',
                         index : 'nextrole',
                         width : 80
                     },
                     {
                         name : 'lastperformer',
                         index : 'lastperformer',
                         width : 60
                     },
                     {
                         name : 'status',
                         index : 'status',
                         width : 80
                     }
                    
                     ],
        rowNum : 50,
        mtype: 'POST',
        rowList : [ 20, 50, 100, 200, 500 ],
        pager : '#pager',
        sortname : 'value',
        datatype : 'xml',
        rownumbers : true,
        viewrecords : true,
        sortorder : "asc",
        scrollOffset : 17,
        multiselect: true,
        loadComplete : function() {
            gGridIdList = RecordGrid.getDataIDs();
            gGridIdListCount = gGridIdList.length;
        	ChangeJQGridAllRowColor(RecordGrid);
        	 $('input[role*="checkbox"]').click(function(){RecordGrid.setSelection($(this).parent().parent().attr('id'));});
        	$("#jqgrid").show();
        },
        onSelectRow : function(id, status) {
            var ids = "" + RecordGrid.jqGrid('getGridParam', 'selarrrow'), idList = ids.split(","), idListLength = idList.length;
            if (ids.indexOf(",") == 0)
                idListLength--;
            if (status == true) {
                var rowdata = RecordGrid.getRowData(id);
                ChangeJQGridSelectMultiRowColor(id, "S");
                if (idListLength == gGridIdListCount)
                    document.getElementById("cb_RecordList").checked = true;
            }
            else if (status == false) {
                ChangeJQGridSelectMultiRowColor(id, "US");
                document.getElementById("cb_RecordList").checked = false;
            }
        },
        onSelectAll: function(id, status) {
        	if(status==false){
        		 var ids = ""+id, idList = ids.split(","), idListLength = idList.length;
        		for(var i = 0; i < idListLength; i++) {
        			ChangeJQGridSelectMultiRowColor(idList[i], "US");
        			}
        	}
        },
        beforeRequest : function() {
        	RecordGrid.setPostDataItem("inpWindowId", document.getElementById("inpWindowId").value);
          }
    });
	RecordGrid.jqGrid('navGrid', '#pager', {
        edit : false,
        add : false,
        del : false,
        search : false,
        view : false
    }, {}, {}, {}, {});
   
	RecordGrid.jqGrid('filterToolbar', {
        searchOnEnter : false
    });
	RecordGrid[0].triggerToolbar();
});

function onClickRefresh() {
	 var action = "";
  	 document.frmMain.action =contextPath+'/sa.elm.ob.utility.ad_forms.ApprovalRevoke.header/ApprovalRevoke?inpAction='+action+'&inpWindowId='+document.getElementById("inpWindowId").value;
   	 document.frmMain.submit();
}

function reSizeGrid() {
	
    if (window.innerWidth) {
        gridW = window.innerWidth - 87;
        gridH = window.innerHeight - 270;
    }
    else if (document.body) {
        gridW = document.body.clientWidth - 87;
        gridH = document.body.clientHeight - 270;
    }
    if (onSearch == 1) {
        gridH = gridH - 23;
        if (navigator.userAgent.toLowerCase().indexOf("webkit") != -1)
            gridH++;
    }
    else if (parseInt(document.getElementById("client").scrollHeight) + 77 > parseInt(document.body.clientHeight))
        gridW = gridW - 14;
    if (gridW < 800)
        gridW = 800;
    if (gridH < 200)
        gridH = 200;
    RecordGrid.setGridWidth(gridW, true);
    RecordGrid.setGridHeight(gridH, true);
}
function onchangeWindow(){
	$('#RecordList').trigger( 'reloadGrid' );
}
function revokeReocrds() {
	 var selectIds = "" + RecordGrid.jqGrid('getGridParam', 'selarrrow'), selectIdList = selectIds.split(","), selectIdListLength = selectIdList.length;
	 var inpWindowId=document.getElementById("inpWindowId").value;
	 if(selectIds===''){
		 OBAlert(OneRecordToRevoke);
	 }
	   OBAsk("Are You Sure to Revoke ?", function(result) {
		   if(result){
				document.frmMain.action = contextPath+'/sa.elm.ob.utility.ad_forms.ApprovalRevoke.header/ApprovalRevoke?inpAction=Revoke&selectIds='+selectIds+'&inpWindowId='+inpWindowId;
			    document.frmMain.submit();
		   }
	   });
}