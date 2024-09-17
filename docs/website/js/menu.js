var navEl = document.getElementsByTagName("NAV")[0];

document.onclick = function(e){
    navEl.classList.remove("mobile");
};

navEl.onclick = function(e){
    e.stopPropagation();
};



function toggleMobileNav() {
    if (navEl.classList.contains("mobile")) {
        navEl.classList.remove("mobile");
    } else {
        navEl.classList.add("mobile");
    }
}
