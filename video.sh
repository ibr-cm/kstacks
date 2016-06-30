ffmpeg -framerate $2 -pattern_type glob -i "./$1/tick*.png" -c:v libx264 -r 30 -pix_fmt yuv420p "$3/out_$2fps.mp4"
