const API_URL = 'http://localhost:7001/'


function linkHeader() {
    const toggleButton = document.getElementById('themeToggle');
    if (toggleButton === null) return;
    toggleButton.addEventListener('click', () => {
        document.documentElement.classList.toggle('dark');
        if(document.documentElement.classList.contains('dark')){
            localStorage.setItem('theme', 'dark');
        } else {
            localStorage.setItem('theme', 'light');
        }
    });

    if (localStorage.getItem('theme') === 'dark'){
        document.documentElement.classList.add('dark');
    }
}

function generateTitle() {
    let t = window.location.href;
    let parts = t.split("/");
    let title = 'DataCat Store';
    if (parts.length >= 4) {
        console.log(parts);
        let site = parts[3];
        if (site !== '') {
            let na = '';
            let j = 0;
            for (let i in site.split('')) {
                if (j === 0) na += site.split('')[i].toUpperCase();
                else na += site.split('')[i].toLowerCase();
                j++;
            }
            title += ' - ' + na;
        }
    }
    document.title = title;
}

function redirectTo(page) {
    let params = '?redirect=' + page;
    window.location.href = '/redirect-ask?dat=' + btoa(params);
}

async function loadHeader() {
    const res = await fetch("/components/header.html");
    document.body.insertAdjacentHTML("beforebegin", await res.text());
}

async function loadFooter() {
    const res = await fetch("/components/footer.html");
    document.body.insertAdjacentHTML("beforeend", await res.text());
}

loadHeader().then(() => linkHeader());
loadFooter();
generateTitle();