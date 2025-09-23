document.addEventListener('DOMContentLoaded', function() {
    // DataTable initialization
    if (typeof jQuery !== 'undefined') {
        $('.datatable').DataTable({
            responsive: true,
            pageLength: 8,
            lengthMenu: [5, 10, 25, 50],
            columns: [
                { orderable: true },
                { orderable: true },
                { orderable: true },
                { orderable: true },
                { orderable: true },
                { orderable: true },
                { orderable: true },
                { orderable: false }
            ],
            language: {
                url: '//cdn.datatables.net/plug-ins/1.10.25/i18n/Spanish.json'
            },
            drawCallback: function() {
                $('.dataTables_paginate > .pagination').addClass('pagination-rounded');
            }
        });
        $('.dataTables_length').hide();
    } else {
        console.error('jQuery or DataTables is not loaded');
    }
});