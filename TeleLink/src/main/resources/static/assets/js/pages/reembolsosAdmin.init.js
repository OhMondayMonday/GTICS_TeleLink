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
radialchart=new ApexCharts(document.querySelector("#radialchart-1"),radialoptions),radialoptions=(radialchart.render(),{series:[65],chart:{type:"radialBar",wight:60,height:60,sparkline:{enabled:!0}},dataLabels:{enabled:!1},colors:["#1cbb8c"],stroke:{lineCap:"round"},plotOptions:{radialBar:{hollow:{margin:0,size:"70%"},track:{margin:0},dataLabels:{show:!1}}}}),options=((radialchart=new ApexCharts(document.querySelector("#radialchart-2"),radialoptions)).render(),{series:[{data:[23,32,27,38,27,32,27,34,26,31,28]}],chart:{type:"line",width:80,height:35,sparkline:{enabled:!0}},stroke:{width:[3],curve:"smooth"},colors:["#5664d2"],tooltip:{fixed:{enabled:!1},x:{show:!1},y:{title:{formatter:function(e){return""}}},marker:{show:!1}}}),options=((chart=new ApexCharts(document.querySelector("#spak-chart1"),options)).render(),{series:[{data:[24,62,42,84,63,25,44,46,54,28,54]}],chart:{type:"line",width:80,height:35,sparkline:{enabled:!0}},stroke:{width:[3],curve:"smooth"},colors:["#5664d2"],tooltip:{fixed:{enabled:!1},x:{show:!1},y:{title:{formatter:function(e){return""}}},marker:{show:!1}}}),options=((chart=new ApexCharts(document.querySelector("#spak-chart2"),options)).render(),{series:[{data:[42,31,42,34,46,38,44,36,42,32,54]}],chart:{type:"line",width:80,height:35,sparkline:{enabled:!0}},stroke:{width:[3],curve:"smooth"},colors:["#5664d2"],tooltip:{fixed:{enabled:!1},x:{show:!1},y:{title:{formatter:function(e){return""}}},marker:{show:!1}}});(chart=new ApexCharts(document.querySelector("#spak-chart3"),options)).render(),$("#usa-vectormap").vectorMap({map:"us_merc_en",backgroundColor:"transparent",regionStyle:{initial:{fill:"#e8ecf4",stroke:"#74788d","stroke-width":1,"stroke-opacity":.4}}}),
    $(document).ready(function() {

        // Inicializar DataTable con configuración para reembolsos
        var table = $(".datatable").DataTable({
            lengthMenu: [5, 10, 25, 50],
            pageLength: 10,
            columns: [
                {orderable: !0}, // Usuario
                {orderable: !0}, // Establecimiento Deportivo
                {orderable: !0}, // Monto
                {orderable: !0}, // Motivo
                {orderable: !0}, // Estado
                {orderable: !0}, // Fecha y Hora
                {orderable: !1}  // Acciones
            ],
            language: {
                "decimal": "",
                "emptyTable": "No hay reembolsos disponibles",
                "info": "Mostrando _START_ a _END_ de _TOTAL_ reembolsos",
                "infoEmpty": "Mostrando 0 a 0 de 0 reembolsos",
                "infoFiltered": "(filtrado de _MAX_ reembolsos totales)",
                "infoPostFix": "",
                "thousands": ",",
                "lengthMenu": "Mostrar _MENU_ reembolsos",
                "loadingRecords": "Cargando...",
                "processing": "Procesando...",
                "search": "Buscar:",
                "zeroRecords": "No se encontraron reembolsos",
                "paginate": {
                    "first": "Primero",
                    "last": "Último",
                    "next": "<i class='mdi mdi-chevron-right'></i>",
                    "previous": "<i class='mdi mdi-chevron-left'></i>"
                },
                "aria": {
                    "sortAscending": ": activar para ordenar ascendente",
                    "sortDescending": ": activar para ordenar descendente"
                }
            },

            drawCallback: function() {
                $(".dataTables_paginate > .pagination").addClass("pagination-rounded");
            }
        });

        $(".dataTables_length").hide();
    });
