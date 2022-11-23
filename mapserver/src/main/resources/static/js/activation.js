function closePop() {
    var close = document.getElementById("popContainer");
    var mask = document.getElementById("mask");
    close.style.display = "none";
    mask.style.display = "none";
}

function showBox() {
    var show = document.getElementById("popContainer");
    var mask = document.getElementById("mask");
    show.style.display = "block"
    mask.style.display = "block";
    $.ajax({
        type: "GET",
        url: "http://127.0.0.1:8088/pygisserver/license/getServerInfos",
        success: function (data) {
            $("#cpuSerial").val(data.items.cpuSerial);
            $("#ipAddress").val(data.items.ipAddress);
            $("#mainBoardSerial").val(data.items.mainBoardSerial);
            $("#macAddress").val(data.items.macAddress);
        }
    });
    // var text = $(".code").find(":text").val();
    $(".active").css({ "background": "#1579d7" })
}

$(document).ready(function () {


    var text = $(".code").find(":text");

    $(".copy").click(
        function copy() {
            var numArr = { "macAddress": [], "mainBoardSerial": [], "ipAddress": [], "cpuSerial": [] };
            var textarea = document.createElement("textarea");
            // for (let i = 0; i < text.length; i++) {
            //     // numArr[i].push(text.eq(i).val());
            //     var trim = text.eq(i).val()
            // }
            // numArr.forEach((item) => {
            //     console.log(item);
            // })
            var macAddress = text.eq(0).val().replace(/\s*/g, "")
            var mainBoardSerial = text.eq(1).val().replace(/\s*/g, "")
            var ipAddress = text.eq(2).val().replace(/\s*/g, "")
            var cpuSerial = text.eq(3).val().replace(/\s*/g, "")
            numArr.macAddress = macAddress.split(",")
            numArr.mainBoardSerial = mainBoardSerial.split(",")
            numArr.ipAddress = ipAddress.split(",")
            numArr.cpuSerial = cpuSerial.split(",")
            console.log(numArr.macAddress);
            textarea.value = JSON.stringify(numArr);
            document.body.appendChild(textarea)
            textarea.focus();
            textarea.setSelectionRange ? textarea.setSelectionRange(0, textarea.value.length) : textarea.select();
            var result = document.execCommand('cut');
            document.body.removeChild(textarea);
            $.message({
                message: '复制成功',
                type: 'success'
            });
        }
    )

    $(".active").click(
        function active() {
            $.ajax({
                type: "GET",
                url: "http://127.0.0.1:8088/pygisserver/license/activateLicense",
                success: function (data) {
                    if (data.msg == 'success') {
                        $.message({
                            message: data.items,
                            type: 'success'
                        });
                        setTimeout(() => { window.open(`http://127.0.0.1:8088/pygisserver/main/home`) }, 2000)

                    } else {
                        $.message({
                            message: data.items,
                            type: 'error'
                        });
                        // alert(data.items);
                    }
                },
            });
        }
    )
})

$(document).ready(function () {

})

$(document).ready(function () {
    var text = $(".code").find(":text");
    setTimeout(() => {
        text.on("input propertychange change",
            function inputAct() {
                if ($("#macAddress").val() == '' || $("#mainBoardSerial").val() == ''
                    || $("#ipAddress").val() == '' || $("#cpuSerial").val() == '') {
                    $(".active").css({ "background": "#999" })
                } else {
                    $(".active").css({ "background": "#1579d7" })
                }
            }
        )
    }, 2000);

})
