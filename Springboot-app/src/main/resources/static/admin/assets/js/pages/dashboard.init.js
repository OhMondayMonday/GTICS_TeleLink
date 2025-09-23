// Variables globales para los gráficos
let reservasChart = null;
let asistenciasChart = null;

// Datos iniciales
const diasSemana = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

// Función para inicializar el gráfico de reservas por día
function initReservasChart() {
    const options = {
        series: [0, 0, 0, 0, 0, 0, 0],
        chart: {
            height: 280,
            type: 'donut',
        },
        labels: diasSemana,
        colors: ['#5664d2', '#1cbb8c', '#eeb902', '#f46a6a', '#50a5f1', '#f1b44c', '#74788d'],
        dataLabels: {
            enabled: true,
            formatter: function (val, opts) {
                return opts.w.config.series[opts.seriesIndex];
            }
        },
        legend: {
            show: false
        },
        plotOptions: {
            pie: {
                donut: {
                    size: '70%'
                }
            }
        },
        responsive: [{
            breakpoint: 480,
            options: {
                chart: {
                    width: 200
                },
                legend: {
                    position: 'bottom'
                }
            }
        }]
    };

    if (reservasChart) {
        reservasChart.destroy();
    }
    
    reservasChart = new ApexCharts(document.querySelector("#reservas-chart"), options);
    reservasChart.render();
}

// Función para inicializar el gráfico de asistencias
function initAsistenciasChart() {
    const options = {
        series: [0, 0, 0, 0],
        chart: {
            height: 280,
            type: 'pie',
        },
        labels: ['Puntual', 'Tardanza', 'Inasistencia', 'Cancelada'],
        colors: ['#1cbb8c', '#eeb902', '#f46a6a', '#74788d'],
        dataLabels: {
            enabled: true,
            formatter: function (val, opts) {
                return opts.w.config.series[opts.seriesIndex];
            }
        },
        legend: {
            show: false
        },
        responsive: [{
            breakpoint: 480,
            options: {
                chart: {
                    width: 200
                }
            }
        }]
    };

    if (asistenciasChart) {
        asistenciasChart.destroy();
    }
    
    asistenciasChart = new ApexCharts(document.querySelector("#asistencias-chart"), options);
    asistenciasChart.render();
}

// Función para cargar datos de reservas
async function cargarReservasPorDia(establecimientoId = '') {
    try {
        const url = establecimientoId ? 
            `/admin/api/reservas-semana?establecimientoId=${establecimientoId}` : 
            '/admin/api/reservas-semana';
            
        const response = await fetch(url);
        const data = await response.json();
        
        // Actualizar gráfico
        const series = diasSemana.map(dia => data[dia] || 0);
        reservasChart.updateSeries(series);
        
        // Actualizar total
        const total = series.reduce((sum, val) => sum + val, 0);
        document.getElementById('totalReservasSemana').textContent = total;
        
    } catch (error) {
        console.error('Error al cargar reservas:', error);
    }
}

// Función para cargar datos de asistencias
async function cargarAsistenciasPorCoordinador(coordinadorId = '') {
    try {
        const url = coordinadorId ? 
            `/admin/api/asistencias-coordinador?coordinadorId=${coordinadorId}` : 
            '/admin/api/asistencias-coordinador';
            
        const response = await fetch(url);
        const data = await response.json();
        
        // Actualizar gráfico
        const series = [
            data.puntual || 0,
            data.tarde || 0,
            data.inasistencia || 0,
            data.cancelada || 0
        ];
        asistenciasChart.updateSeries(series);
        
        // Actualizar contadores
        document.getElementById('contadorPuntual').textContent = data.puntual || 0;
        document.getElementById('contadorTarde').textContent = data.tarde || 0;
        document.getElementById('contadorInasistencia').textContent = data.inasistencia || 0;
        document.getElementById('contadorCancelada').textContent = data.cancelada || 0;
        
    } catch (error) {
        console.error('Error al cargar asistencias:', error);
    }
}

// Inicialización cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', function() {
    // Inicializar gráficos
    initReservasChart();
    initAsistenciasChart();
    
    // Cargar datos iniciales
    cargarReservasPorDia();
    cargarAsistenciasPorCoordinador();
    
    // Event listeners para filtros
    document.getElementById('filtroEstablecimiento').addEventListener('change', function() {
        cargarReservasPorDia(this.value);
    });
    
    document.getElementById('filtroCoordinador').addEventListener('change', function() {
        cargarAsistenciasPorCoordinador(this.value);
    });
});

// Gráfico original de líneas y columnas (mantenido)
var options = {
    series: [
        { name: "2020", type: "column", data: [23, 42, 35, 27, 43, 22, 17, 31, 22, 22, 12, 16] },
        { name: "2019", type: "line", data: [23, 32, 27, 38, 27, 32, 27, 38, 22, 31, 21, 16] }
    ],
    chart: { height: 280, type: "line", toolbar: { show: !1 } },
    stroke: { width: [0, 3], curve: "smooth" },
    plotOptions: { bar: { horizontal: !1, columnWidth: "20%" } },
    dataLabels: { enabled: !1 },
    legend: { show: !1 },
    colors: ["#5664d2", "#1cbb8c"],
    labels: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
};

// Solo renderizar si el elemento existe
const lineColumnElement = document.querySelector("#line-column-chart");
if (lineColumnElement) {
    var chart = new ApexCharts(lineColumnElement, options);
    chart.render();
}

// Resto de gráficos radiales y sparklines (mantenidos)
var radialoptions = {
    series: [72],
    chart: { type: "radialBar", width: 60, height: 60, sparkline: { enabled: !0 } },
    dataLabels: { enabled: !1 },
    colors: ["#5664d2"],
    stroke: { lineCap: "round" },
    plotOptions: { radialBar: { hollow: { margin: 0, size: "70%" }, track: { margin: 0 }, dataLabels: { show: !1 } } }
};

const radialElement1 = document.querySelector("#radialchart-1");
if (radialElement1) {
    var radialchart = new ApexCharts(radialElement1, radialoptions);
    radialchart.render();
}

radialoptions = {
    series: [65],
    chart: { type: "radialBar", width: 60, height: 60, sparkline: { enabled: !0 } },
    dataLabels: { enabled: !1 },
    colors: ["#1cbb8c"],
    stroke: { lineCap: "round" },
    plotOptions: { radialBar: { hollow: { margin: 0, size: "70%" }, track: { margin: 0 }, dataLabels: { show: !1 } } }
};

const radialElement2 = document.querySelector("#radialchart-2");
if (radialElement2) {
    var radialchart2 = new ApexCharts(radialElement2, radialoptions);
    radialchart2.render();
}

// Sparkline charts
const sparkElements = [
    { selector: "#spak-chart1", data: [23, 32, 27, 38, 27, 32, 27, 34, 26, 31, 28] },
    { selector: "#spak-chart2", data: [24, 62, 42, 84, 63, 25, 44, 46, 54, 28, 54] },
    { selector: "#spak-chart3", data: [42, 31, 42, 34, 46, 38, 44, 36, 42, 32, 54] }
];

sparkElements.forEach(spark => {
    const element = document.querySelector(spark.selector);
    if (element) {
        const options = {
            series: [{ data: spark.data }],
            chart: { type: "line", width: 80, height: 35, sparkline: { enabled: !0 } },
            stroke: { width: [3], curve: "smooth" },
            colors: ["#5664d2"],
            tooltip: { fixed: { enabled: !1 }, x: { show: !1 }, y: { title: { formatter: function(e) { return "" } } }, marker: { show: !1 } }
        };
        const sparkChart = new ApexCharts(element, options);
        sparkChart.render();
    }
});

// DataTable initialization
$(document).ready(function() {
    if ($(".datatable").length > 0) {
        $(".datatable").DataTable({
            lengthMenu: [5, 10, 25, 50],
            pageLength: 5,
            columns: [
                { orderable: !1 },
                { orderable: !0 },
                { orderable: !0 },
                { orderable: !0 },
                { orderable: !0 },
                { orderable: !0 },
                { orderable: !1 }
            ],
            order: [[1, "asc"]],
            language: { paginate: { previous: "<i class='mdi mdi-chevron-left'>", next: "<i class='mdi mdi-chevron-right'>" } },
            drawCallback: function() {
                $(".dataTables_paginate > .pagination").addClass("pagination-rounded");
            }
        });
        $(".dataTables_length select").addClass("form-select form-select-sm");
    }
});