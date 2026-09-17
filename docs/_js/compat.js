/* Global variable to indicate when the search function is available */
var searchready = false;
/* md compatibility list lines split as rows */
var rowdata;
/* Compatibility chart colors */
var bgcolors=['#6ba544','#2a7fc0','#c0c044', '#c08444', '#c04a4a'];
var colors=['#81d41a','#0066ff','#ffff00', '#ff6600', '#ff0000'];
/* Amount of apps in each compatibility state */
        /* ['Perfect','Minor issues','Playable','Ingame','Not booting'] */
var values=[0, 0, 0, 0, 0];
var total_apps = 0;

/* Variables to filter entries by compatibility state */
var perfect_enabled = true;
var minor_issue_enabled = true;
var playable_enabled = true;
var ingame_enabled = true;
var not_booting_enabled = true;

/* 
 * Once the window loads, get the md with the compatibility list and prepare
 * to parse it, as well as to create the charts.
 */
window.onload = function () {
  readMD();
}

/* Biggest function of the entire website, tasked of building the compatibility list */
function readMD() {
  /* Fetch the md file and begin processing it */
  fetch('../compat_data/FreeJ2ME Compatibility.md').then(response => response.text())
  .then(mdFile => {
    /* Split md lines as rows */
    rowdata = mdFile.split('\n');
    generateCompatData();

    
    /* Add data to the buttons and app counter */
  
    /* A "for" loop isn't really needed here since we'll only have 5 status categories */
    total_apps = values[0]+values[1]+values[2]+values[3]+values[4];
    document.getElementById('total_apps').textContent += total_apps;
  
    document.getElementById('b_perfect').textContent += values[0];
    document.getElementById('b_minor_issue').textContent += values[1];
    document.getElementById('b_playable').textContent += values[2];
    document.getElementById('b_ingame').textContent += values[3];
    document.getElementById('b_not_booting').textContent += values[4];
  
    /* Draw donut chart */
    generatePieGraph('chart_canvas', {
      animation: true, 
      animationSpeed: 10, 
      fillTextData: true,
      fillTextColor: '#fff',
      fillTextAlign: 1.25,
      fillTextPosition: 'inner',
      doughnutHoleSize: 60,
      doughnutHoleColor: '#1a1a1aff',
      offset: 0, 
      pie: 'normal',
      values:values,
      colors:colors
    });
  
    /* Draw inner donut chart */
    generatePieGraph('innerchart_canvas', {
      animation: true, 
      animationSpeed: 10,
      fillTextData: true,
      fillTextColor: '#fff',
      fillTextAlign: 1.5,
      fillTextPosition: 'inner',
      doughnutHoleSize: 35,
      doughnutHoleColor: '#1a1a1aff',
      offset: 0, 
      pie: 'normal',
      values:[(values[0]+values[1]+values[2]), values[3], values[4]],
      colors:["#3faf46", colors[3], colors[4]]
    });
  
    /* CSV has been parsed and the compatibility list is ready. Allow the user to search. */
    searchready = true;
  });
}

/* Helper function to generate the compatibility date separate from the main md function */
function generateCompatData() {
  var statcolor = '', maindivname='', elem_bordercolor='';
  var compat_table = document.getElementById('compat_table');
  var i = 0;
  var temp_elements = '';

  /* Empty the compat_table contents beforehand, useful in case of a regen call. */
  compat_table.innerHTML = '';

  /* For each entry on the compatibility list: */
  for (row of rowdata) {

    /* Last row of md is always empty, so treat that case */
    if(row.length > 0) {
      // The CSV is formatted for better readability on github and text
      // editors now, so we must trim the empty spaces here.
      var line = row.trim();

      // Columns are separated by '|' in the md
      if (line.startsWith('|')) { line = line.substring(1); }
      if (line.endsWith('|')) { line = line.substring(0, line.length - 1); }

      columndata = line.split('|');

      switch(columndata[2].toLowerCase().trim()) {
        case 'no issues':
          statcolor = 'background-color:' + bgcolors[0] + ';';
          elem_bordercolor = 'border: 3px solid ' + colors[0] + ';';
          values[0] +=1;
          break;
        case 'minor issues':
          statcolor = 'background-color:' + bgcolors[1] + ';';
          elem_bordercolor = 'border: 3px solid ' + colors[1] + ';';
          values[1] +=1;
          break;
        case 'playable':
          statcolor = 'background-color:' + bgcolors[2] + ';';
          elem_bordercolor = 'border: 3px solid ' + colors[2] + ';';
          values[2] +=1;
          break;
        case 'intro/menu':
          statcolor = 'background-color:' + bgcolors[3] + ';';
          elem_bordercolor = 'border: 3px solid ' + colors[3] + ';';
          values[3] +=1;
          break;
        case 'unplayable':
          statcolor = 'background-color:' + bgcolors[4] + ';';
          elem_bordercolor = 'border: 3px solid ' + colors[4] + ';';
          values[4] +=1;
          break;
        default: /* Skip any invalid entries */
          continue;
      }

      /* Names now have special markers to represent the platform they are built for (MIDP, DoJa, etc.) */
      var rawName = columndata[0].trim();
      var formattedName = rawName.replace(/\[([^\]]+)\]/g, function(match, platformText) {
        var platClass = platformText.toLowerCase().replace(/[^a-z0-9]/g, '_');
        return '<span class="platform_badge platform_' + platClass + '">•' + platformText + '•</span>';
      });


      /* We are now embedding compat flags, etc. into the description as well, so format them. */
      var rawDesc = columndata[3] ? columndata[3].trim() : '';
      var formattedDesc = '';

      if (rawDesc.indexOf('Required Settings:') !== -1) {
        var parts = rawDesc.split('Required Settings:');
        var mainText = parts[0].trim();
        var settingsRaw = parts[1].trim();

        if (mainText.length > 0) {
          formattedDesc += '<div>' + mainText + '</div>';
        }

        if (settingsRaw.length > 0) {
          var settingsArray = settingsRaw.split(',');
          formattedDesc += '<div class="req_settings_title"><b>Required Settings:</b></div>';
          formattedDesc += '<ul class="req_settings_list">';
          for (var s = 0; s < settingsArray.length; s++) {
            var settingItem = settingsArray[s].trim();
            if (settingItem.length > 0) {
              // Anything with double quotes becomes a badge for better readability.
              settingItem = settingsArray[s].trim().replace(/["›“]([^"‹”]+)["‹”]/g, '<span class="setting_val">$1</span>');
              formattedDesc += '<li><b>' + settingItem + '</b></li>';
            }
          }
          formattedDesc += '</ul>';
        }
      } else {
        formattedDesc = rawDesc;
      }
      
      /* Inserts each row's data into the expected div */
      maindivname = 'id="compat_entry' + i + '"';

      temp_elements += '\
      <div class="compat_entry" ' + maindivname + ' style="' + elem_bordercolor +  '">' + '\n \
        <div id="entryname">' + formattedName + '</div>\
        <div id="entryres">'  + columndata[1].trim() + '</div>\
        <div id="entrystat"><div id="statbg" style="' + statcolor + ' ' + elem_bordercolor +  '">' + columndata[2].trim() + '</div></div>\
        <div id="entrydesc">' + formattedDesc + '</div>\
        <div id="entryupd"><div id="extrabg">'  + columndata[4].trim() + '</div></div>\
        <div id="entrymd5"><div id="extrabg">'  + columndata[5] + '</div></div>\
      </div>';

      i+=1;
    }
  }

  /* 
   * Only effectively add all elements to the page after parsing everything. This avoids multiple 
   * costly calls to concatenate text into 'compat_table.innerHTML'. 
   */
  compat_table.innerHTML += temp_elements;
}

/* Function that implements the search function for the compatibility list */
function searchApp() {
  if(searchready) {
    var searchcontents = document.getElementById('appsearch').value.toLowerCase();
    var i;
    var compat_entry, entryname;

    /* 
     * No need to re-add elements to DOM, just hide everything that doesn't include the search string
     * and show everything that includes it. It's much faster and also shows all elements if the string
     * is empty.
     */
    for (i = 0; i < total_apps; i++) {
      compat_entry = document.getElementById(`compat_entry${i}`);
      /* The name of any given entry is on the very first element of compat_entry */
      entryname = compat_entry.children[0];

      if(entryname.innerHTML.toLowerCase().includes(searchcontents)) {
        compat_entry.style.display = "flex";
      }
      else {
        compat_entry.style.display = "none";
      }
    }
  }
}

function updateCompatState() {
  var i;
  var compat_entry, entrystat;

  for (i = 0; i < total_apps; i++) {
    compat_entry = document.getElementById(`compat_entry${i}`);

    /* 
     * The compat status of any given entry is on the second element of compat_entry,
     * but it has a div inside of it, so the text is even further in.
     */
    entrystat = compat_entry.children[2].children[0];

    /* Make all entries begin as 'display: none' to significantly shorten the conditionals below. */
    compat_entry.style.display = "none";

    if(entrystat.textContent.toLowerCase() === "no issues" && perfect_enabled) {
        compat_entry.style.display = "flex";

    } else if(entrystat.textContent.toLowerCase() === "minor issues" && minor_issue_enabled) {
        compat_entry.style.display = "flex";

    } else if(entrystat.textContent.toLowerCase() === "playable" && playable_enabled) {
        compat_entry.style.display = "flex";

    } else if(entrystat.textContent.toLowerCase() === "intro/menu" && ingame_enabled) {
        compat_entry.style.display = "flex";

    } else if(entrystat.textContent.toLowerCase() === "unplayable" && not_booting_enabled) {
        compat_entry.style.display = "flex";
    }
  }
}

function toggleStatus(status) {
  if (status === 'no_issue') {
    
    if(perfect_enabled) {
      document.getElementById('b_perfect').style.backgroundColor = 'transparent';
      document.getElementById('b_perfect').style.color = 'white';
    } else {
      document.getElementById('b_perfect').style.backgroundColor = bgcolors[0];
      document.getElementById('b_perfect').style.color = 'black';
    }
    perfect_enabled = !perfect_enabled;

  } else if (status === 'minor_issue') {
    
    if(minor_issue_enabled) {
      document.getElementById('b_minor_issue').style.backgroundColor = 'transparent';
      document.getElementById('b_minor_issue').style.color = 'white';
    } else {
      document.getElementById('b_minor_issue').style.backgroundColor = bgcolors[1];
      document.getElementById('b_minor_issue').style.color = 'black';
    }
    minor_issue_enabled = !minor_issue_enabled;

  } else if (status === 'playable') {
    
    if(playable_enabled) {
      document.getElementById('b_playable').style.backgroundColor = 'transparent';
      document.getElementById('b_playable').style.color = 'white';
    } else {
      document.getElementById('b_playable').style.backgroundColor = bgcolors[2];
      document.getElementById('b_playable').style.color = 'black';
    }
    playable_enabled = !playable_enabled;

  } else if (status === 'intro_menu') {
    
    if(ingame_enabled) {
      document.getElementById('b_ingame').style.backgroundColor = 'transparent';
      document.getElementById('b_ingame').style.color = 'white';
    } else {
      document.getElementById('b_ingame').style.backgroundColor = bgcolors[3];
      document.getElementById('b_ingame').style.color = 'black';
    }
    ingame_enabled = !ingame_enabled;

  } else if (status === 'unplayable') {
    
    if(not_booting_enabled) {
      document.getElementById('b_not_booting').style.backgroundColor = 'transparent';
      document.getElementById('b_not_booting').style.color = 'white';
    } else {
      document.getElementById('b_not_booting').style.backgroundColor = bgcolors[4];
      document.getElementById('b_not_booting').style.color = 'black';
    }
    not_booting_enabled = !not_booting_enabled;

  }

  /* Update the compatibility list with the filters */
  updateCompatState();
}
