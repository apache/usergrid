;;
;; If you've installed Nu (http://github.com/timburks/nu),
;; then you can build the Usergrid SDK as a Mac framework
;; by typing "nuke" in this directory. Type "nuke install"
;; to install it in /Library/Frameworks.
;; 
(set @m_files (filelist "^UGAPI/.*.m$"))
(@m_files addObjectsFromSet:(filelist "^UGAPI/SBJSON/.*.m$"))
(@m_files addObjectsFromSet:(filelist "^UGAPI/v2/.*.m$"))

(set @arch '("x86_64"))

;; framework description
(set @framework "Usergrid")
(set @framework_identifier "com.apigee.usergrid")

(set @cc "clang")
(set @cflags "-fobjc-arc -I UGAPI -I UGAPI/SBJSON -I UGAPI/v2")
(set @ldflags  "-framework Foundation ")

(compilation-tasks)
(framework-tasks)

(task "clobber" => "clean" is
      (SH "rm -rf #{@framework_dir}"))

(task "default" => "framework")


