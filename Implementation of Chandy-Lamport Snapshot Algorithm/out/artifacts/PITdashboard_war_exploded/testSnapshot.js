var maxSnapshots = 10;
var snapshotInterval = 1500;
var numSnapshot;
var tbl;
var commodities=[];
var snapshotTimer; // Hold reference to the interval timer to enable cancelling
var halting = false;


$(function() { // when document is ready
    $("#start").submit(pitInit);
    $("#halt").submit(pitHalt);
});

function pitInit() {
    halting = false;
    $("#initArea").empty();
    tbl = $("<table border=1 cellpadding=20>");

    numSnapshot = 0;
    try {
        $.ajax({
            url: "PITsnapshot",
            type: "post",
            dataType: "json"
        })
                .done(pitInitReply)
                .fail(function(jqxhr, textStatus, error) {
                    var err = textStatus + ", " + error;
                    console.log("PITinit Request Failed: " + err);
                    $("#initArea").append("<h2>Start simulation failed</h2>");
                });
        return false;
    } catch (e) {
        console.log("Error in PITinit GET: " + e.description);
        $("#initArea").append(e.description);
        return false;
    }
}

function pitInitReply(data) {
    $("#initArea").html(data.message);
    commodities = data.commodities;
    console.log("commodities: " + commodities);
    snapshotTimer = setTimeout(takeSnapshot, snapshotInterval);
}

function pitHalt() {
    clearTimeout(snapshotTimer);
    halting = true;
    try {
        $.ajax({
            url: "PITsnapshot",
            type: "delete",
            dataType: "json"
        })
                .done(pitHaltReply)
                .fail(function(jqxhr, textStatus, error) {
                    var err = textStatus + ", " + error;
                    console.log("PITinit Request Failed: " + err);
                    $("#initArea").append("<h2>Halt simulation failed</h2>");
                });
        return false;
    } catch (e) {
        console.log("Error in PITinit DELETE: " + e.description);
        $("#initArea").append(e.description);
        return false;
    }
}

function pitHaltReply(data) {
    $("#initArea").append(data.message);
}

function takeSnapshot() {
    if (halting) return;
    try {
        $.get("PITsnapshot", pitSnapshotReply);
        return false;
    } catch (e) {
        console.log(e.description);
        $("#initArea").append(e.description);
        return false;
    }
}

function pitSnapshotReply(data) {
    if (halting) return;
    if (data == "Snapshot Failed") {
        $("#initArea").append("<h2>Snapshot #" + ++numSnapshot + " Failed</h2><br>");
        if (numSnapshot < maxSnapshots)
            setTimeout(takeSnapshot, snapshotInterval);
        return;
    }
    var commoditySums={};
    var headers = '<tr align=center><td>Sum</td>';
    commodities.forEach(function(commodity) {
        commoditySums[commodity]=0;
        $(data).find("."+commodity).each(function() {
            commoditySums[commodity] += (parseInt($(this).text()) || 0)
        });
        headers += '<td>' + commoditySums[commodity] +'</td>';
    });
    headers += '</tr>';

    data = $(data).append(headers);
    $("#initArea").append("<h2>Snapshot #" + ++numSnapshot + "</h2>");
    $("#initArea").append(data);
    $("#initArea").append("<br>");
    if (numSnapshot < maxSnapshots)
        setTimeout(takeSnapshot, snapshotInterval);
}
