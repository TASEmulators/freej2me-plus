/*
 * script.js
 *
 * Research In Motion Limited proprietary and confidential
 * Copyright Research In Motion Limited, 2009-2009
 * 
 */

function openInNewWindow(url) {
	window.open(url, "popup", "height=420,width=600" );
}

function init() {
	var col = document.getElementsByName("categoryCheckbox");
	for (i=0; i<col.length; ++i) {
		var name = col[i].name;
		var value = false;
		if (document.cookie.indexOf(name) != -1) {
			value = getValue(name);
			if(value == "false") {
				col[i].checked = false;
			} else {
				col[i].checked = true;
			}
			var elements = document.getElementsByName(name.substring(4));

			for (j = 0; j < elements.length; ++j) {
				var parent = elements[j].parentNode;
				if ( parent.nodeName == "TD" ) parent = parent.parentNode; //get the parent again to get the enclosing row
				if ( parent.nodeName != "TR" && parent.nodeName != "DIV" ) break; //not an element we can handle :(
				parent.style.display = (col[i].checked == true) ? "" : "none";
			}
		}
	}
}

function getValue(name)
{
	var start = document.cookie.indexOf("=", document.cookie.indexOf(name)) + 1;
	var end = document.cookie.indexOf(";", start);
	if (end < 0) {
		end = document.cookie.length;
	}
	return document.cookie.substring(start, end);
}

function toggleElements(elementName)
{
	var sourceCheckBox = document.getElementsByName( "show" + elementName )[0];
	if (sourceCheckBox.checked == true) {
		document.cookie = sourceCheckBox.name + "=true;";
	} else {
		document.cookie = sourceCheckBox.name + "=false;";
	}
	var elements = document.getElementsByName(elementName);
	var output = document.getElementById("debug");
	if ( output != null ) {
		output.innerText = ":toggleElements():elements.length= " + elements.length;
		output.innerText += ":source=" + sourceCheckBox.type;
		output.innerText += ":elementName=" + elementName;
	}
	for (i = 0; i < elements.length; ++i) {
		var parent = elements[i].parentNode;
		if ( output != null ) output.innerText += "parent.nodeName=" + parent.nodeName;
		if ( parent.nodeName == "TD" ) parent = parent.parentNode; //get the parent again to get the enclosing row
		if ( parent.nodeName != "TR" && parent.nodeName != "DIV" ) break; //not an element we can handle :(
		if ( output != null ) output.innerText += " node.innerText=" + parent.innerText + " node.style.display=" + parent.style.display;
		parent.style.display = (sourceCheckBox.checked == true) ? "" : "none";
	}
}
