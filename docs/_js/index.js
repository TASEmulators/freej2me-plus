/* This function runs when the page loads in order to animate a few elements */
function loadPage() 
{

    /* After it loads, prepare screen animations */
    var animlogo = document.getElementById('init-logo');
    var animpage = document.getElementById('mainpage');

    animlogo.addEventListener("animationend", function() 
    {
        animlogo.style.display = "none";
        animpage.style.display = "block";
    }.bind(animlogo));
}

document.addEventListener("DOMContentLoaded", loadPage);