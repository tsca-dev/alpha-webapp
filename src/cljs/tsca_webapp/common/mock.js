export function sleep(x, result){
    console.log("wait: " + x);
    return new Promise(function(resolve, reject){
        window.setTimeout(function(){
            console.log("wait done: " + x);
            resolve(result);
        }, x);
    });
}

export function cancelableSleep(time, result){
    console.log("wait: " + time);
    return function(){
        var cancelId;
        var _reject;
        var p = new Promise(function(resolve, reject){
            _reject = reject;
            cancelId = window.setTimeout(function(){
                console.log("wait done: " + time);
                resolve(result);
            }, time);
        });

        return { promise: p, cancel: function(){
            clearTimeout(cancelId);
            _reject("sleep canceled");
        }};
    }();
}
