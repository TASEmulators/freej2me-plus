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

function startWebPlayer() 
{
    document.getElementById('firstpane').style.opacity = "0";
    document.getElementById('firstpane').style.maxHeight = "100vh";
    document.getElementById('firstpane').style.height = "100vh";
    setTimeout(() => {
        document.getElementById('firstpane').style.display = 'none';
        document.getElementById('web_player_frame').style.display = 'block';
        document.getElementById('web_player_frame').style.opacity = '0'; 
        setTimeout(() => 
        { 
            document.getElementById('web_player_frame').style.opacity = '1'; 
        }, 16);
    }, 500);
}

document.addEventListener("DOMContentLoaded", loadPage);