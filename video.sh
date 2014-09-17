#mkdir images
#rm out.mp4
#for image in *.png
#do
#	cp $image ./images/"${image%%.*}".png
#	cp $image ./images/"${image%%.*}"1.png
#	cp $image ./images/"${image%%.*}"2.png
#	cp $image ./images/"${image%%.*}"3.png
#	cp $image ./images/"${image%%.*}"4.png
#	cp $image ./images/"${image%%.*}"5.png
#done

# ffmpeg -f image2 -pattern_type glob -i "./images/tick_*.png" -r 20 output.mpg
ffmpeg -framerate 50 -pattern_type glob -i "./tick*.png" -c:v libx264 -r 30 -pix_fmt yuv420p out_5fps.mp4
#ffmpeg -framerate 10 -pattern_type glob -i "./tick*.png" -c:v libx264 -r 30 -pix_fmt yuv420p out_10fps.mp4

#rm *.jpg
#rm *.png
#rm -r images
