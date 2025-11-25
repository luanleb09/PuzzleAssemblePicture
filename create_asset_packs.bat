@echo off
for /L %%i in (1,1,15) do (
    set "num=00%%i"
    set "num=!num:~-3!"
    mkdir puzzlepack_!num!\src\main\assets
    (
        echo plugins {
        echo     id 'com.android.asset-pack'
        echo }
        echo.
        echo assetPack {
        echo     packName = "puzzlepack_!num!"
        echo     dynamicDelivery {
        echo         deliveryType = "on-demand"
        echo     }
        echo }
    ) > puzzlepack_!num!\build.gradle
)
echo Done!
pause