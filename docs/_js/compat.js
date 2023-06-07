/* Global variable to indicate when the search function is available */
var searchready = false;
/* csv compatibility list lines split as rows */
var rowdata;
/* Compatibility chart colors */
var colors=['#81d41a','#729fcf','#ffff38', '#ff8000', '#ff0000'];
/* Amount of apps in each compatibility state */
        /* ['Perfect','Minor issues','Playable','Ingame','Not booting'] */
var values=[0, 0, 0, 0, 0];

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
    document.getElementById('total_apps').textContent += (values[0]+values[1]+values[2]+values[3]+values[4]);
  
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
  var add_entry = false;

  /* Empty the compat_table contents beforehand, useful in case of a regen call. */
  compat_table.innerHTML = '';

  /* For each entry on the compatibility list: */
  for (row of rowdata) {

    /* Last row of csv is always empty, so treat that case */
    if(row.length > 0) {
      /* Columns are separated by '|' in the csv */
      columndata = row.split('|');

      /* Filter entry based on enabled compatibility states */
      add_entry = false;

      /* TODO: Improve the text detection here, as any minor deviation can make a pass fail */
      switch(columndata[2]) {
        case 'Perfect':
          statcolor = 'style="background-color:' + colors[0] + ';"';
          elem_bordercolor = 'style="border-color:' + colors[0] + ';"';
          if(perfect_enabled) { add_entry = true; values[0] +=1; }
          break;
        case 'Minor issues':
          statcolor = 'style="background-color:' + colors[1] + ';"';
          elem_bordercolor = 'style="border-color:' + colors[1] + ';"';
          if(minor_issue_enabled) { add_entry = true; values[1] +=1; }
          break;
        case 'Playable':
          statcolor = 'style="background-color:' + colors[2] + ';"';
          elem_bordercolor = 'style="border-color:' + colors[2] + ';"';
          if(playable_enabled) { add_entry = true; values[2] +=1; }
          break;
        case 'Ingame':
          statcolor = 'style="background-color:' + colors[3] + ';"';
          elem_bordercolor = 'style="border-color:' + colors[3] + ';"';
          if(ingame_enabled) { add_entry = true; values[3] +=1; }
          break;
        case 'Not booting':
          statcolor = 'style="background-color:' + colors[4] + ';"';
          elem_bordercolor = 'style="border-color:' + colors[4] + ';"';
          if(not_booting_enabled) { add_entry = true; values[4] +=1; }
          break;
        default: /* Skip any invalid entries */
          continue;
      }
      
      if(add_entry) {
        /* Inserts each row's data into the expected div */
        if(((values[0]+values[1]+values[2]+values[3]+values[4])) %2 == 0) {
          maindivname = 'id="compat_entry0"';
        }
        else {
          maindivname = 'id="compat_entry1"';
        }
        
        addElements(compat_table, columndata, compat_table, maindivname, elem_bordercolor, statcolor);
      }

    }
  }
}

/* Function that implements the search function for the compatibility list */
function searchApp() {
  if(searchready) {
    var searchcontents = document.getElementById('appsearch').value.toLowerCase();
    var statcolor = '', elem_bordercolor='', animdelay='';
    var i = 0, values = [0, 0, 0, 0, 0];
    var compat_table = document.getElementById('compat_table');
    var add_entry = false;

    /* Empty the compatibility list in order to add only relevant results */
    compat_table.innerHTML = '';

    /* Turns out using includes() to match strings allows us to not regen the whole table with an empty string */
    for (row of rowdata) {
      add_entry = false;
        /* Last row of csv is always empty, so treat that case */
      if(row.length > 0) {
        columndata = row.split('|');

        if(columndata[0].toLowerCase().includes(searchcontents)) {
          i+=1;

          /* TODO: Improve the text detection here, as any minor deviation can make a pass fail */
          switch(columndata[2]) {
            case 'Perfect':
              statcolor = 'style="background-color:' + colors[0] + ';"';
              elem_bordercolor = 'style="border-color:' + colors[0] + ';"';
              if(perfect_enabled) { add_entry = true; values[0] +=1; }
              break;
            case 'Minor issues':
              statcolor = 'style="background-color:' + colors[1] + ';"';
              elem_bordercolor = 'style="border-color:' + colors[1] + ';"';
              if(minor_issue_enabled) { add_entry = true; values[1] +=1; }
              break;
            case 'Playable':
              statcolor = 'style="background-color:' + colors[2] + ';"';
              elem_bordercolor = 'style="border-color:' + colors[2] + ';"';
              if(playable_enabled) { add_entry = true; values[2] +=1; }
              break;
            case 'Ingame':
              statcolor = 'style="background-color:' + colors[3] + ';"';
              elem_bordercolor = 'style="border-color:' + colors[3] + ';"';
              if(ingame_enabled) { add_entry = true; values[3] +=1; }
              break;
            case 'Not booting':
              statcolor = 'style="background-color:' + colors[4] + ';"';
              elem_bordercolor = 'style="border-color:' + colors[4] + ';"';
              if(not_booting_enabled) { add_entry = true; values[4] +=1; }
              break;
            default: /* Skip any invalid entries */
              continue;
          }

          /* Only filter by compat status when the search string is empty. */
          if(add_entry || searchcontents !== '') {
            /* Inserts each row's data into the expected div */
            if(i%2 == 0) {
              maindivname = 'id="compat_entry0"';
            }
            else {
              maindivname = 'id="compat_entry1"';
            }
            
            addElements(compat_table, columndata, compat_table, maindivname, elem_bordercolor, statcolor);
          }

        }
      }
    }
  }
}

/* Helper function to add elements to the compat table DOM */
function addElements(compat_table, columndata, compat_table, maindivname, elem_bordercolor, statcolor) {
  compat_table.innerHTML += '\
    <div class="compat_entry" ' + maindivname + elem_bordercolor +  '>' + '\n \
      <div id="entryname">' + columndata[0] + '</div>\
      <div id="entryres">'  + columndata[1] + '</div>\
      <div id="entrystat"><div id="statbg" ' + statcolor + '>' + columndata[2] + '</div></div>\
      <div id="entrydesc">' + columndata[3] + '</div>\
      <div id="entryupd"><div id="extrabg">'  + columndata[4] + '</div></div>\
      <div id="entrymd5"><div id="extrabg">'  + columndata[5] + '</div></div>\
    </div>';
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
  generateCompatData();
}