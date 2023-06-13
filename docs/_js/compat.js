/* Global variable to indicate when the search function is available */
var searchready = false;
/* csv compatibility list lines split as rows */
var rowdata;
/* Compatibility chart colors */
var colors=['#81d41a','#729fcf','#ffff38', '#ff8000', '#ff0000'];
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
 * Once the window loads, get the csv with the compatibility list and prepare
 * to parse it, as well as to create the charts.
 */
window.onload = function () {
  readCSV();
}

/* Biggest function of the entire website, tasked of building the compatibility list */
function readCSV() {
  /* Fetch the csv file and begin processing it */
  fetch('../compat_data/FreeJ2ME Compatibility.csv').then(response => response.text()) 
  .then(csvFile => {
    /* Split csv lines as rows */
    rowdata = csvFile.split('\n');
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
      fillTextColor: '#222',
      fillTextAlign: 1.25,
      fillTextPosition: 'inner', 
      doughnutHoleSize: 60,
      doughnutHoleColor: '#fff',
      offset: 0, 
      pie: 'normal',
      values:values,
      colors:colors
    });
  
    /* Draw inner donut chart */
    generatePieGraph('innerchart_canvas', {
      animation: true, 
      animationSpeed: 15, 
      fillTextData: true,
      fillTextColor: '#222',
      fillTextAlign: 1.5,
      fillTextPosition: 'inner', 
      doughnutHoleSize: 40,
      doughnutHoleColor: '#fff',
      offset: 0, 
      pie: 'normal',
      values:[(values[0]+values[1]+values[2]), values[3], values[4]],
      colors:["#3faf46", colors[3], colors[4]]
    });
  
    /* CSV has been parsed and the compatibility list is ready. Allow the user to search. */
    searchready = true;
  });
}

/* Helper function to generate the compatibility date separate from teh main csv function */
function generateCompatData() {
  var statcolor = '', maindivname='', elem_bordercolor='';
  var compat_table = document.getElementById('compat_table');
  var i = 0;
  var temp_elements = '';

  /* Empty the compat_table contents beforehand, useful in case of a regen call. */
  compat_table.innerHTML = '';

  /* For each entry on the compatibility list: */
  for (row of rowdata) {

    /* Last row of csv is always empty, so treat that case */
    if(row.length > 0) {
      /* Columns are separated by '|' in the csv */
      columndata = row.split('|');

      /* TODO: Improve the text detection here, as any minor deviation can make a pass fail */
      switch(columndata[2]) {
        case 'Perfect':
          statcolor = 'style="background-color:' + colors[0] + ';"';
          elem_bordercolor = 'style="border-color:' + colors[0] + ';"';
          values[0] +=1;
          break;
        case 'Minor issues':
          statcolor = 'style="background-color:' + colors[1] + ';"';
          elem_bordercolor = 'style="border-color:' + colors[1] + ';"';
          values[1] +=1;
          break;
        case 'Playable':
          statcolor = 'style="background-color:' + colors[2] + ';"';
          elem_bordercolor = 'style="border-color:' + colors[2] + ';"';
          values[2] +=1;
          break;
        case 'Ingame':
          statcolor = 'style="background-color:' + colors[3] + ';"';
          elem_bordercolor = 'style="border-color:' + colors[3] + ';"';
          values[3] +=1;
          break;
        case 'Not booting':
          statcolor = 'style="background-color:' + colors[4] + ';"';
          elem_bordercolor = 'style="border-color:' + colors[4] + ';"';
          values[4] +=1;
          break;
        default: /* Skip any invalid entries */
          continue;
      }
      
      /* Inserts each row's data into the expected div */
      maindivname = 'id="compat_entry' + i + '"';

      temp_elements += '\
      <div class="compat_entry" ' + maindivname + elem_bordercolor +  '>' + '\n \
        <div id="entryname">' + columndata[0] + '</div>\
        <div id="entryres">'  + columndata[1] + '</div>\
        <div id="entrystat"><div id="statbg" ' + statcolor + '>' + columndata[2] + '</div></div>\
        <div id="entrydesc">' + columndata[3] + '</div>\
        <div id="entryupd"><div id="extrabg">'  + columndata[4] + '</div></div>\
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

    if(entrystat.textContent.toLowerCase() === "perfect" && perfect_enabled) {
        compat_entry.style.display = "flex";

    } else if(entrystat.textContent.toLowerCase() === "minor issues" && minor_issue_enabled) {
        compat_entry.style.display = "flex";

    } else if(entrystat.textContent.toLowerCase() === "playable" && playable_enabled) {
        compat_entry.style.display = "flex";

    } else if(entrystat.textContent.toLowerCase() === "ingame" && ingame_enabled) {
        compat_entry.style.display = "flex";

    } else if(entrystat.textContent.toLowerCase() === "not booting" && not_booting_enabled) {
        compat_entry.style.display = "flex";
    }
  }
}

function toggleStatus(status) {
  if (status === 'perfect') {
    
    if(perfect_enabled) {
      document.getElementById('b_perfect').style.backgroundColor = 'transparent';
    } else {
      document.getElementById('b_perfect').style.backgroundColor = colors[0];
      document.getElementById('b_perfect').style.borderColor = colors[0];
    }
    perfect_enabled = !perfect_enabled;

  } else if (status === 'minor_issue') {
    
    if(minor_issue_enabled) {
      document.getElementById('b_minor_issue').style.backgroundColor = 'transparent';
    } else {
      document.getElementById('b_minor_issue').style.backgroundColor = colors[1];
    }
    minor_issue_enabled = !minor_issue_enabled;

  } else if (status === 'playable') {
    
    if(playable_enabled) {
      document.getElementById('b_playable').style.backgroundColor = 'transparent';
    } else {
      document.getElementById('b_playable').style.backgroundColor = colors[2];
    }
    playable_enabled = !playable_enabled;

  } else if (status === 'ingame') {
    
    if(ingame_enabled) {
      document.getElementById('b_ingame').style.backgroundColor = 'transparent';
    } else {
      document.getElementById('b_ingame').style.backgroundColor = colors[3];
    }
    ingame_enabled = !ingame_enabled;

  } else if (status === 'not_booting') {
    
    if(not_booting_enabled) {
      document.getElementById('b_not_booting').style.backgroundColor = 'transparent';
    } else {
      document.getElementById('b_not_booting').style.backgroundColor = colors[4];
    }
    not_booting_enabled = !not_booting_enabled;

  }

  /* Update the compatibility list with the filters */
  updateCompatState();
}