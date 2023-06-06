# Piechart
Javascript pie chart with less than 5kb without any dependency like css and js library. We can generate 10 different pie chart such as stroke pie, doughnut pie etc. 

For generate the pie chart, we need to pass 2 arguments such as id of the canvas and an object with key value pair in the "generatePieGraph()" just like below-

HTML
----
```
<canvas id="myCanvas" width="300" height="300"></canvas>
```
JavaScript Object
-----------------
```
var obj = {
	pieId: 'myPieCanvas',
	values: [15, 50, 20, 85, 30],
	colors: ['#4CAF50', '#00BCD4', '#E91E63', '#FFC107', '#9E9E9E'],
	animation: true, // Takes boolean value & default behavious is false
	animationSpeed: 50, // Time in miliisecond & default animation speed is 20ms
	fillTextData: true, // Takes boolean value & text is not generate by default 
	fillTextColor: '#fff', // For Text colour & default colour is #fff (White)
	fillTextAlign: 1.30, // for alignment of inner text position i.e. higher values gives closer view to center & default text alignment is 1.85 i.e closer to center
	fillTextPosition: 'inner', // 'horizontal' or 'vertical' or 'inner' & default text position is 'horizontal' position i.e. outside the canvas
	doughnutHoleSize: 50, // Percentage of doughnut size within the canvas & default doughnut size is null
	doughnutHoleColor: '#fff', // For doughnut colour & default colour is #fff (White)
	offset: 1, // Offeset between two segments & default value is null
	pie: 'normal', // if the pie graph is single stroke then we will add the object key as "stroke" & default is normal as simple as pie graph
	isStrokePie: { 
		stroke: 20, // Define the stroke of pie graph. It takes number value & default value is 20
		overlayStroke: true, // Define the background stroke within pie graph. It takes boolean value & default value is false
		overlayStrokeColor: '#eee', // Define the background stroke colour within pie graph & default value is #eee (Grey)
		strokeStartEndPoints: 'Yes', // Define the start and end point of pie graph & default value is No
		strokeAnimation: true, // Used for animation. It takes boolean value & default value is true
		strokeAnimationSpeed: 40, // Used for animation speed in miliisecond. It takes number & default value is 20ms
		fontSize: '60px', // Used to define text font size & default value is 60px
		textAlignement: 'center', // Used for position of text within the pie graph & default value is 'center'
		fontFamily: 'Arial', // Define the text font family & the default value is 'Arial'
		fontWeight: 'bold' //  Define the font weight of the text & the default value is 'bold'
	}
};
```            
To generate the pie chart
-------------------------
```
generatePieGraph('myCanvas', obj);
```

Examples
--------
![](https://repository-images.githubusercontent.com/223873892/9fa26380-104c-11ea-840b-e35bd353b982)
