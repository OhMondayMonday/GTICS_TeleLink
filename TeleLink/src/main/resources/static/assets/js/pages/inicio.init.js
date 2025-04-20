var options = {
    series: [{name:"2020",type:"column",data:[23,42,35,27,43,22,17,31,22,22,12,16]}, {name:"2019",type:"line",data:[23,32,27,38,27,32,27,38,22,31,21,16]}],
    chart: {height:280,type:"line",toolbar:{show:!1}},
    stroke: {width:[0,3],curve:"smooth"},
    plotOptions: {bar:{horizontal:!1,columnWidth:"20%"}},
    dataLabels: {enabled:!1},
    legend: {show:!1},
    colors: ["#5664d2","#1cbb8c"],
    labels: ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"]
};

var chart = new ApexCharts(document.querySelector("#line-column-chart"), options);
chart.render();

// MODIFICACIÓN PARA EL DONUT CHART (parte que necesitas cambiar)
var donutEl = document.querySelector("#donut-chart");
var puntualidad = donutEl ? parseInt(donutEl.dataset.puntualidad) || 89 : 89;
var tardanzas = donutEl ? parseInt(donutEl.dataset.tardanzas) || 11 : 11;

options = {
    series: [100*puntualidad/(puntualidad+tardanzas), 100*tardanzas/(puntualidad+tardanzas)],
    chart: {height:250,type:"donut"},
    labels: ["Puntualidad", "Tardanzas"],
    plotOptions: {pie:{donut:{size:"75%"}}},
    dataLabels: {enabled:!1},
    legend: {show:!1},
    colors: ["#34c38f","#f46a6a"]
};

chart = new ApexCharts(donutEl, options);
chart.render();

// El resto del código permanece igual
radialoptions = {
    series: [72],
    chart: {type:"radialBar",wight:60,height:60,sparkline:{enabled:!0}},
    dataLabels: {enabled:!1},
    colors: ["#5664d2"],
    stroke: {lineCap:"round"},
    plotOptions: {radialBar:{hollow:{margin:0,size:"70%"},track:{margin:0},dataLabels:{show:!1}}}
};

!function(v){"use strict";function e(){}e.prototype.init=function(){
    var a=v("#event-modal"),
        t=v("#modal-title"),
        n=v("#form-event"),
        l=null,
        i=null,
        r=document.getElementsByClassName("needs-validation"),
        l=null,
        i=null,
        currentDate=new Date(),
        s=currentDate.getDate(),
        d=currentDate.getMonth(),
        e=currentDate.getFullYear(),
        calendarEl=document.getElementById("calendar");
    
    function o(e){a.modal("show"),n.removeClass("was-validated"),n[0].reset(),
                  v("#event-title").val(""),
                  v("#event-date").val(""),
                  v("#event-start-time").val(""),
                  v("#event-end-time").val(""),
                  v("#event-category").val(""),
                  t.text("Agregar Evento"),i=e}
  
    var c=new FullCalendar.Calendar(calendarEl,{
        plugins:["bootstrap","interaction","dayGrid","timeGrid"],
        editable:!1,
        droppable:!1,
        selectable:!1,
        defaultView:"timeGridWeek",
        themeSystem:"bootstrap",
        allDaySlot: false,
        locale: 'es',
        slotMinTime: "00:00:00",
        slotMaxTime: "24:00:00",
        header:{left:"prev,next today",center:"title",right:"dayGridMonth,timeGridWeek,timeGridDay"},
        buttonText: {
            today: 'Hoy',
            month: 'Mes',
            week: 'Semana',
            day: 'Día'
        },
        eventClick:function(e){
            e.jsEvent.preventDefault();
            return false;
        },
        events:[
          {
            title: 'Complejo Deportivo Bancario: Cancha de fútbol',
            startTime: '08:00',
            endTime: '15:00',
            daysOfWeek: [0, 1, 2, 3, 4, 5, 6], // Todos los días (0=domingo)
            startRecur: new Date().toISOString().split('T')[0], // Comienza hoy
            endRecur: '2025-04-30', // Finaliza el 30 de abril de 2025
            className: 'bg-success'
          }
        ]
    });
    
    c.render(),
    v(n).on("submit",function(e){
        e.preventDefault();
        var eventTitle = v("#event-title").val(),
            eventDate = v("#event-date").val(),
            startTime = v("#event-start-time").val(),
            endTime = v("#event-end-time").val(),
            category = v("#event-category").val();
        
        if(!1===r[0].checkValidity()){
            e.preventDefault();
            e.stopPropagation();
            r[0].classList.add("was-validated");
            return;
        }
        
        var startDateTime = new Date(eventDate + 'T' + startTime);
        var endDateTime = new Date(eventDate + 'T' + endTime);
        
        var eventData = {
            title: eventTitle,
            start: startDateTime,
            end: endDateTime,
            className: category,
            allDay: false
        };
        
        if(l){
            l.setProp('title', eventData.title);
            l.setDates(eventData.start, eventData.end);
            l.setProp('classNames', [eventData.className]);
        }else{
            c.addEvent(eventData);
        }
        
        a.modal("hide");
    }),
    v("#btn-delete-event").on("click",function(e){l&&(l.remove(),l=null,a.modal("hide"))}),
    v("#btn-new-event").on("click",function(e){o({date:new Date,allDay:!0})})
  },
  v.CalendarPage=new e,v.CalendarPage.Constructor=e}(window.jQuery),
  function(){"use strict";window.jQuery.CalendarPage.init()}();