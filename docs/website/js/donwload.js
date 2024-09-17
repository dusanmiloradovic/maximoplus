var form = document.getElementById("form");
var errorText = document.getElementById("errorpost");
var spinner = document.getElementById("loading");
if (form) {
  form.onsubmit = function(ev) {
    ev.preventDefault();
    spinner.classList.remove("invisible");
    spinner.classList.add("loading");
    var data = new FormData(form);
    var req = new XMLHttpRequest();
    req.open("POST", "/srv/postform");
    req.onload = function() {
      spinner.classList.remove("loading");
      spinner.classList.add("invisible");
      if (req.status == 200) {
        errorText.innerHTML =
          "<div class='noerror'>" + req.responseText + "</div>";
        window.location = "/srv/download";
      } else {
        errorText.innerHTML =
          "<div class='error'>ERROR: " + req.statusText + "</div>";
      }
    };
    req.onerror = function(ev) {
      spinner.classList.remove("loading");
      spinner.classList.add("invisible");
      errorText.innerHTML =
        "<div class='error'>Error in sending, please try again later.</div>";
    };
    req.send(data);
  };
}
