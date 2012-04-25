

var socket = new WebSocket("ws://localhost:8080");

var msgobj;

socket.onmessage = function(message) {
    var msgbox = $("#message");
    msgobj = message;
    msgbox.html(message.data);
}


$("#tosend").change(function() {
    var message = $("#tosend").val();
    alert("sent: " + message);
    socket.send(message);
});


$("#joinbutton").click(function() {
    var game = $("#joingame").val();
    alert("joining " + game);
    socket.send("{:type :join :game :" + game + "}");
});


$("#name").change(function() {
    var name = $("#name").val();
    socket.send("{:type :name-change :name" + "\"" + name + "\""  + "}");
});

