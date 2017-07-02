<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Scegliere modalità</title>
</head>
<body>
 <form method='GET' action='/MyWebProject/GenerateAndDownloadHash'>
 	<input type="hidden" value="not-recursive" name="mode" />
    <input type="submit" value="Non ricorsivo">
 </form>
 <form method='GET' action='/MyWebProject/GenerateAndDownloadHash'>
 	<input type="hidden" value="recursive" name="mode" />
    <input type="submit" value="Ricorsivo">
 </form>
 <form method='GET' action='/MyWebProject/GenerateAndDownloadHash'>
 	<input type="hidden" value="no-subfolders" name="mode" />
    <input type="submit" value="Senza sottocartelle">
 </form>
</body>
</html>
