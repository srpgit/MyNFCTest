$(function(){

    showNfcInfo();

    function showNfcInfo(){
        var data = tp.getData();
        if(data){
            console.log(data);
            data = $.parseJSON(data);
            for(var k in data){
                $('<tr>').append($('<td>').html(k)).append($('<td>').html(data[k])).appendTo($('#nfc-info'));
            }
        }
    }
});