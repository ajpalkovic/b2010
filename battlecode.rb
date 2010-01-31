ROOT = ENV['USERPROFILE']+"/"

def forkAndRun(map, opponent)
  cmd = "#{File.join(FileUtils.pwd, 'ant.bat')} run > #{File.join(FileUtils.pwd, "#{map}-#{opponent}-output.txt")} &\n"
  output = system "#{cmd}"
  sleep(3)
end

def updateConfig(map, opponent)
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
  File.open("#{ROOT}.battlecode.ui", "w+") do |f|
    f.write text
  end
end

def run()
  maps = ENV['maps'].split(',')
  opponents = ENV['opponents'].split(',')
  
  if opponents.size > 1
    for map in maps
      for opponent in opponents
          updateConfig(map, opponent)
          forkAndRun(map, opponent)
      end
      puts "Press enter to move onto the next map."
      gets unless map == maps[-1]
    end
  else
    for opponent in opponents
      for map in maps
          updateConfig(map, opponent)
          forkAndRun(map, opponent)
      end
      puts "Press enter to move onto the next opponent."
      gets unless opponent == opponents[-1]
    end
  end
end

FileUtils.cp("#{ROOT}.battlecode.ui", "#{ROOT}.battlecode.ui.backup")
FileUtils.cp(File.join(FileUtils.pwd, "bc.conf"), File.join(FileUtils.pwd, "bc.conf.backup"))
FileUtils.cp(File.join(FileUtils.pwd, "bc.conf.noDialog"), File.join(FileUtils.pwd, "bc.conf"))
run
FileUtils.cp("#{ROOT}.battlecode.ui.backup", "#{ROOT}.battlecode.ui")
FileUtils.cp(File.join(FileUtils.pwd, "bc.conf.backup"), File.join(FileUtils.pwd, "bc.conf"))