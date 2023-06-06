/* Global variable to indicate when the search function is available */
var searchready = false;
/* csv compatibility list lines split as rows */
var rowData;
/* Compatibility chart colors */
var colors=['#81d41a','#729fcf','#ffff38', '#ff8000', '#ff0000'];

/* 
 * Once the window loads, get the csv with the compatibility list and prepare
 * to parse it, as well as to create the charts.
 */
window.onload = function () {
  readCSV();
}

/* Biggest function of the entire website, tasked of building the compatibility list */
function readCSV() {
  /* Amount of apps in each compatibility state */
          /* ['Perfect','Minor issues','Playable','Ingame','Not booting'] */
  var values=[0, 0, 0, 0, 0];

  /* Fetch the csv file and begin processing it */
  fetch('../compat_data/FreeJ2ME Compatibility.csv').then(response => response.text()) 
  .then(csvFile => {
      /* Split csv lines as rows */
      rowData = csvFile.split('\n');
      generateCompatData(values);

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


/* TODO: Implement this search function */
function searchApp() {
  if(searchready) {
    var searchcontents = document.getElementById('appsearch').value.toLowerCase();
    var statcolor = '';
    var i = 0;

    /* Empty the compatibility list in order to add only relevant results */
    document.getElementById('compat_table').innerHTML = '';

    /* If the search string is empty, regenerate the whole compatibility list */
    if(searchcontents === '') {
      var values=[0, 0, 0, 0, 0]; /* Stub in order to reuse the function below */
      generateCompatData(values);
    }
    else { /* If it isn't, search for relevant entries in the list. TODO: Find a way of better reusing the code below. */
      for (row of rowData) {
        columnData = row.split('|');

        if(columnData[0].toLowerCase().includes(searchcontents)) {
          i+=1;
          /* TODO: Improve the text detection here, as any minor deviation can make a pass fail */
          switch(columnData[2]) {
            case 'Perfect':
              statcolor = 'style="background-color:' + colors[0] + ';"';
              break;
            case 'Minor issues':
              statcolor = 'style="background-color:' + colors[1] + ';"';
              break;
            case 'Playable':
              statcolor = 'style="background-color:' + colors[2] + ';"';
              break;
            case 'Ingame':
              statcolor = 'style="background-color:' + colors[3] + ';"';
              break;
            case 'Not booting':
              statcolor = 'style="background-color:' + colors[4] + ';"';
              break;
            default: /* Skip any invalid entries */
              continue;
          }

          /* Inserts each row's data into the expected div */
          if(i%2 == 0) {
            maindivname = 'id="compat_entry0"';
          }
          else {
            maindivname = 'id="compat_entry1"';
          }
          document.getElementById('compat_table').innerHTML += '\
          <div ' + maindivname + '>' + '\n \
            <div id="entryname">' + columnData[0] + '</div>\
            <div id="entryres">'  + columnData[1] + '</div>\
            <div id="entrystat"><div id="statbg" ' + statcolor + '>' + columnData[2] + '</div></div>\
            <div id="entrydesc">' + columnData[3] + '</div>\
            <div id="entryupd"><div id="extrabg">'  + columnData[4] + '</div></div>\
            <div id="entrymd5"><div id="extrabg">'  + columnData[5] + '</div></div>\
          </div>';
        }
      }
    }
  }
}

/* Helper function to generate the compatibility date separate from teh main csv function */
function generateCompatData(values) {
  var statcolor = '', maindivname=''; 
  var totalApps = 0;

  for (row of rowData) {
        
    /* Last row of csv is always empty, so treat that case */
    if(row.length > 0) {

      /* Columns are separated by '|' in the csv */
      columnData = row.split('|');

      /* TODO: Improve the text detection here, as any minor deviation can make a pass fail */
      switch(columnData[2]) {
        case 'Perfect':
          values[0] +=1;
          statcolor = 'style="background-color:' + colors[0] + ';"';
          break;
        case 'Minor issues':
          values[1] +=1;
          statcolor = 'style="background-color:' + colors[1] + ';"';
          break;
        case 'Playable':
          values[2] +=1;
          statcolor = 'style="background-color:' + colors[2] + ';"';
          break;
        case 'Ingame':
          values[3] +=1;
          statcolor = 'style="background-color:' + colors[3] + ';"';
          break;
        case 'Not booting':
          values[4] +=1;
          statcolor = 'style="background-color:' + colors[4] + ';"';
          break;
        default: /* Skip any invalid entries */
          continue;
      }

      /* Inserts each row's data into the expected div */
      if(((values[0]+values[1]+values[2]+values[3]+values[4])) %2 == 0) {
        maindivname = 'id="compat_entry0"';
      }
      else {
        maindivname = 'id="compat_entry1"';
      }
      document.getElementById('compat_table').innerHTML += '\
      <div ' + maindivname + '>' + '\n \
        <div id="entryname">' + columnData[0] + '</div>\
        <div id="entryres">'  + columnData[1] + '</div>\
        <div id="entrystat"><div id="statbg" ' + statcolor + '>' + columnData[2] + '</div></div>\
        <div id="entrydesc">' + columnData[3] + '</div>\
        <div id="entryupd"><div id="extrabg">'  + columnData[4] + '</div></div>\
        <div id="entrymd5"><div id="extrabg">'  + columnData[5] + '</div></div>\
      </div>';
    }
  }
}