ffmpeg -framerate 5 -pattern_type glob -i "./tick*.png" -c:v libx264 -r 30 -pix_fmt yuv420p out_5fps.mp4
ffmpeg -framerate 10 -pattern_type glob -i "./tick*.png" -c:v libx264 -r 30 -pix_fmt yuv420p out_10fps.mp4
ffmpeg -framerate 50 -pattern_type glob -i "./tick*.png" -c:v libx264 -r 30 -pix_fmt yuv420p out_50fps.mp4
