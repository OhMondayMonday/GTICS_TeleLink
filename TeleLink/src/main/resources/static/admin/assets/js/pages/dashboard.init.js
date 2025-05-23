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
var chart = new ApexCharts(document.querySelector("#line-column-chart"), options);
chart.render();

// Comentar el gráfico de dona en #donut-chart para evitar conflicto
/*
options = {
  series: [42, 26, 15],
  chart: { height: 250, type: "donut" },
  labels: ["Product A", "Product B", "Product C"],
  plotOptions: { pie: { donut: { size: "75%" } } },
  dataLabels: { enabled: !1 },
  legend: { show: !1 },
  colors: ["#5664d2", "#1cbb8c", "#eeb902"]
};
chart = new ApexCharts(document.querySelector("#donut-chart"), options);
chart.render();
*/

var radialoptions = {
    series: [72],
    chart: { type: "radialBar", width: 60, height: 60, sparkline: { enabled: !0 } },
    dataLabels: { enabled: !1 },
    colors: ["#5664d2"],
    stroke: { lineCap: "round" },
    plotOptions: { radialBar: { hollow: { margin: 0, size: "70%" }, track: { margin: 0 }, dataLabels: { show: !1 } } }
};
var radialchart = new ApexCharts(document.querySelector("#radialchart-1"), radialoptions);
radialchart.render();

radialoptions = {
    series: [65],
    chart: { type: "radialBar", width: 60, height: 60, sparkline: { enabled: !0 } },
    dataLabels: { enabled: !1 },
    colors: ["#1cbb8c"],
    stroke: { lineCap: "round" },
    plotOptions: { radialBar: { hollow: { margin: 0, size: "70%" }, track: { margin: 0 }, dataLabels: { show: !1 } } }
};
radialchart = new ApexCharts(document.querySelector("#radialchart-2"), radialoptions);
radialchart.render();

options = {
    series: [{ data: [23, 32, 27, 38, 27, 32, 27, 34, 26, 31, 28] }],
    chart: { type: "line", width: 80, height: 35, sparkline: { enabled: !0 } },
    stroke: { width: [3], curve: "smooth" },
    colors: ["#5664d2"],
    tooltip: { fixed: { enabled: !1 }, x: { show: !1 }, y: { title: { formatter: function(e) { return "" } } }, marker: { show: !1 } }
};
chart = new ApexCharts(document.querySelector("#spak-chart1"), options);
chart.render();

options = {
    series: [{ data: [24, 62, 42, 84, 63, 25, 44, 46, 54, 28, 54] }],
    chart: { type: "line", width: 80, height: 35, sparkline: { enabled: !0 } },
    stroke: { width: [3], curve: "smooth" },
    colors: ["#5664d2"],
    tooltip: { fixed: { enabled: !1 }, x: { show: !1 }, y: { title: { formatter: function(e) { return "" } } }, marker: { show: !1 } }
};
chart = new ApexCharts(document.querySelector("#spak-chart2"), options);
chart.render();

options = {
    series: [{ data: [42, 31, 42, 34, 46, 38, 44, 36, 42, 32, 54] }],
    chart: { type: "line", width: 80, height: 35, sparkline: { enabled: !0 } },
    stroke: { width: [3], curve: "smooth" },
    colors: ["#5664d2"],
    tooltip: { fixed: { enabled: !1 }, x: { show: !1 }, y: { title: { formatter: function(e) { return "" } } }, marker: { show: !1 } }
};
chart = new ApexCharts(document.querySelector("#spak-chart3"), options);
chart.render();

$("#usa-vectormap").vectorMap({
    map: "us_merc_en",
    backgroundColor: "transparent",
    regionStyle: {
        initial: { fill: "#e8ecf4", stroke: "#74788d", "stroke-width": 1, "stroke-opacity": .4 }
    }
});

$(document).ready(function() {
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
});