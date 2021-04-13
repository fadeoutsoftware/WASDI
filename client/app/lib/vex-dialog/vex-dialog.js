
    function vexTerminalPromptWriteIt(from, event) {
    event = event || window.event;
    var w = document.getElementById("writer");
    var tw = from.value;
    w.innerHTML = tw.replace(/\n/g, "<br />");
};

    function vexTerminalPromptMoveIt(count, e) {

    e = e || window.event;
    var keycode = e.keyCode || e.which;

    if (keycode == 37 && parseInt(cursor.style.left) >= (0 - ((count - 1) * 10))) {
    cursor.style.left = parseInt(cursor.style.left) - 10 + "px";
} else if (keycode == 39 && (parseInt(cursor.style.left) + 10) <= 0) {
    cursor.style.left = parseInt(cursor.style.left) + 10 + "px";
}
};

    function SetCaretAtEnd(elem) {
    var elemLen = elem.value.length;
    // For IE Only
    if (document.selection) {
    // Set focus
    elem.focus();
    // Use IE Ranges
    var oSel = document.selection.createRange();
    // Reset position to 0 & then set at end
    oSel.moveStart('character', -elemLen);
    oSel.moveStart('character', elemLen);
    oSel.moveEnd('character', 0);
    oSel.select();
} else if (elem.selectionStart || elem.selectionStart == '0') {
    // Firefox/Chrome
    elem.selectionStart = elemLen;
    elem.selectionEnd = elemLen;
    elem.focus();
} // if
} // SetCaretAtEnd()

    function vexTerminalPromptFocus() {
    var w = document.getElementById("setter");
    w.focus();
    SetCaretAtEnd(w);
};

