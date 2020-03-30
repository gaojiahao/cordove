/**
 * 把Rose项目开发的web文件打包进来。
 */
const fs = require('fs'),
      path = require('path'),
      sourcePath = path.resolve('../../Rose/apk'),
      target = path.resolve(__dirname,'../www');

del(target,()=>{
   copy(sourcePath);
});

    
function copy(dirPath){
    var fn = arguments.callee;

    fs.readdir(dirPath,(err,paths)=>{
        var filePath,
            l = paths.length,
            i = 0;

        if(err){
            console.log(err.message);
            return;
        }
        loop();

        function loop(){
            var fullFilePath,
                toPath,
                fullTargetPath;

            if(i<l){
                filePath = paths[i];
                fullFilePath = path.resolve(dirPath,filePath);
                toPath = path.relative(sourcePath,fullFilePath);
                fullTargetPath = path.resolve(target,toPath);

                fs.stat(fullFilePath,(err,stat)=>{
                    if(err){
                        console.log(err.message);
                        return;
                    }
                    if(stat.isDirectory()){ //目录
                        if(fs.existsSync(fullTargetPath)){
                            fn(fullFilePath);
                        }else {
                            fs.mkdir(fullTargetPath,()=>{
                                fn(fullFilePath);
                            })
                        }
                    } else {
                        console.log(fullTargetPath);
                        fs.createReadStream(fullFilePath).pipe(fs.createWriteStream(fullTargetPath));
                    } 
                    i++;
                    loop();//下一个
                });
            }
        }

    })
}

function del(dirPath,cb){
    var fullFilePath,
        fn = arguments.callee;

    fs.readdir(dirPath,(err,paths)=>{
        var filePath,
            l = paths.length,
            i = 0;

        if(err){
            console.log(err.message);
            if(cb)cb(err);
            return;
        }

        loop();

        function loop(){
            if(i<l){
                filePath = paths[i];
                fullFilePath = path.resolve(dirPath,filePath);
                fs.stat(fullFilePath,(err,stat)=>{
                    if(stat.isDirectory()) {
                        fn(fullFilePath,function(){
                            fs.rmdir(fullFilePath,()=>{
                                i++;
                                loop();
                            });
                        });    
                    }else{
                        //fs.createReadStream(fullFilePath).pipe(fs.createWriteStream(fullTargetPath));
                        fs.unlink(fullFilePath,()=>{
                            i++;
                            loop();
                        });
                    }
                })  
            } else if(i==l){
                if(cb)cb();
            }
        }
    })
}