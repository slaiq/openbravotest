isc.DynamicForm.create({
    ID: "contactForm",
    width: 400,
    fields: [
        { name: "firstName", title: "First name", type: "text",
          mask: ">?<??????????????", hint: "&gt;?&lt;??????????????"},
        { name: "lastName", title: "Last name", type: "text",
          mask: ">?<??????????????", hint: "&gt;?&lt;??????????????"},
        { name: "state", title: "State", type: "text",
          mask: ">LL", hint: "&gt;LL"},
        { name: "phoneNo", title: "Phone no", type: "text",
          mask: "(###) ###-####", hint: "(###)&nbsp;###-####", showHintInField: true}
    ]
});
