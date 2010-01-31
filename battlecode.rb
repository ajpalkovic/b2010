require 'rubygems'

maps = ENV['maps'].split(',')
opponents = ENV['opponents'].split(',')

def forkAndRun(map, opponent)
  cmd = "#{File.join(FileUtils.pwd, 'ant.bat')} run > #{File.join(FileUtils.pwd, "#{map}-#{opponent}-output.txt")} &\n"
  output = system "#{cmd}"
end

root = ENV['USERPROFILE']+"/"
FileUtils.cp("#{root}.battlecode.ui", "#{root}.battlecode.ui.backup")
for opponent in opponents
  for map in maps
      text = "#ui options
#Sun Jan 31 13:40:18 EST 2010
file=
analyzeFile=false
glclient=true
showMinimap=false
choice=LOCAL
MAP=bridge.xml
save=false
maps=#{map}.xml
TEAM_B=team298
TEAM_A=#{opponent}
save-file=
lastVersion=1.1.12
host=
lockstep=false"
      File.open("#{root}.battlecode.ui", "w+") do |f|
        f.write text
      end
      
      forkAndRun(map, opponent)
      sleep(3)
  end
end
FileUtils.cp("#{root}.battlecode.ui.backup", "#{root}.battlecode.ui")