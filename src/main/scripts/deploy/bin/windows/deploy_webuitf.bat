@echo on

@rem - Replace default configuration files with local-machine specific files.

set WEBUI_TMP_DIR=%SGTEST_CHECKOUT_FOLDER%\apps\webuitf

@echo exporting webuitf

@if %BRANCH_NAME%==trunk (
	set SVN_WEBUITF_REPOSITORY=svn://pc-lab14/SVN/xap/trunk/quality/frameworks/webuitf
) else ( 
	set SVN_WEBUITF_REPOSITORY=svn://pc-lab14/SVN/xap/branches/%SVN_BRANCH_DIRECTORY%/%BRANCH_NAME%/quality/frameworks/webuitf
)

@rem for /f "tokens=2" %%i in ('svn info -rHEAD %SVN_WEBUITF_REPOSITORY%^|find "Revision"') do @set REVISION=%%i
@rem set /p PREV_REVISION=<\\tarzan\tgrid\webuitf.revision
@rem if %REVISION% == %PREV_REVISION% goto:_skip
@rem @echo %REVISION% > \\tarzan\tgrid\webuitf.revision


@mkdir %WEBUI_TMP_DIR%
@svn export %SVN_WEBUITF_REPOSITORY% %WEBUI_TMP_DIR% --force

@echo deploying webuitf...
pushd %WEBUI_TMP_DIR%
mvn clean install s3client:deploy -U
popd


:_skip