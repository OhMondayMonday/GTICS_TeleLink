!function (v) {
    "use strict";

    function e() {
    }

    e.prototype.init = function () {
        var a = v("#event-modal"), t = v("#modal-title"), n = v("#form-event"), l = null, i = null,
            r = document.getElementsByClassName("needs-validation"), l = null, i = null, e = new Date, s = e.getDate(),
            d = e.getMonth(), e = e.getFullYear(),
            s = (new FullCalendarInteraction.Draggable(document.getElementById("external-events"), {
                itemSelector: ".external-event",
                eventData: function (e) {
                    return {title: e.innerText, className: v(e).data("class")}
                }
            }), [{title: "All Day Event", start: new Date(e, d, 1)}, {
                title: "Long Event",
                start: new Date(e, d, s - 5),
                end: new Date(e, d, s - 2),
                className: "bg-warning"
            }, {
                id: 999,
                title: "Repeating Event",
                start: new Date(e, d, s - 3, 16, 0),
                allDay: !1,
                className: "bg-info"
            }, {
                id: 999,
                title: "Repeating Event",
                start: new Date(e, d, s + 4, 16, 0),
                allDay: !1,
                className: "bg-primary"
            }, {
                title: "Meeting",
                start: new Date(e, d, s, 10, 30),
                allDay: !1,
                className: "bg-success"
            }, {
                title: "Lunch",
                start: new Date(e, d, s, 12, 0),
                end: new Date(e, d, s, 14, 0),
                allDay: !1,
                className: "bg-danger"
            }, {
                title: "Birthday Party",
                start: new Date(e, d, s + 1, 19, 0),
                end: new Date(e, d, s + 1, 22, 30),
                allDay: !1,
                className: "bg-success"
            }, {
                title: "Click for Google",
                start: new Date(e, d, 28),
                end: new Date(e, d, 29),
                url: "http://google.com/",
                className: "bg-dark"
            }]), e = (document.getElementById("external-events"), document.getElementById("calendar"));

        function o(e) {
            a.modal("show"), n.removeClass("was-validated"), n[0].reset(), v("#event-title").val(), v("#event-category").val(), t.text("Add Event"), i = e
        }

        var c = new FullCalendar.Calendar(e, {
            plugins: ["bootstrap", "interaction", "dayGrid", "timeGrid"],
            editable: !0,
            droppable: !0,
            selectable: !0,
            defaultView: "dayGridMonth",
            themeSystem: "bootstrap",
            header: {
                left: "prev,next today",
                center: "title",
                right: "dayGridMonth,timeGridWeek,timeGridDay,listMonth"
            },
            eventClick: function (e) {
                a.modal("show"), n[0].reset(), l = e.event, v("#event-title").val(l.title), v("#event-category").val(l.classNames[0]), i = null, t.text("Edit Event"), i = null
            },
            dateClick: function (e) {
                o(e)
            },
            events: s
        });
        c.render(), v(n).on("submit", function (e) {
            e.preventDefault();
            v("#form-event :input");
            var e = v("#event-title").val(), t = v("#event-category").val();
            !1 === r[0].checkValidity() ? (event.preventDefault(), event.stopPropagation(), r[0].classList.add("was-validated")) : (l ? (l.setProp("title", e), l.setProp("classNames", [t])) : (e = {
                title: e,
                start: i.date,
                allDay: i.allDay,
                className: t
            }, c.addEvent(e)), a.modal("hide"))
        }), v("#btn-delete-event").on("click", function (e) {
            l && (l.remove(), l = null, a.modal("hide"))
        }), v("#btn-new-event").on("click", function (e) {
            o({date: new Date, allDay: !0})
        })
    }, v.CalendarPage = new e, v.CalendarPage.Constructor = e
}(window.jQuery), function () {
    "use strict";
    window.jQuery.CalendarPage.init()
}();