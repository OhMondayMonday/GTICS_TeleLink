$(document).ready(function() {
    // Inicializar DataTable con solo el buscador
    var table = $(".datatable").DataTable({
        lengthMenu: [5, 10, 25, 50],
        pageLength: 6,
        columns: [
            { orderable: true },  // Nombre
            { orderable: true },  // Servicio Deportivo
            { orderable: true },  // Estado
            { orderable: false }  // Acción
        ],
        language: {
            "decimal": "",
            "emptyTable": "No hay datos disponibles",
            "info": "Mostrando _START_ a _END_ de _TOTAL_ registros",
            "infoEmpty": "Mostrando 0 a 0 de 0 registros",
            "infoFiltered": "(filtrado de _MAX_ registros totales)",
            "infoPostFix": "",
            "thousands": ",",
            "lengthMenu": "Mostrar _MENU_ registros",
            "loadingRecords": "Cargando...",
            "processing": "Procesando...",
            "search": "Buscar:",
            "zeroRecords": "No se encontraron coincidencias",
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