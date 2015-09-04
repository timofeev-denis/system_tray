<%
On Error Resume Next
Result = ""
Response.Write( "CreateObject: " )
Set OraDBSession = CreateObject("OracleInProcServer.XOraSession")
If Err.Number <> 0 Then
	Response.Write( "Ошибка " & Err.Number & ": " & Err.Description & ". " )
ElseIf NOT IsObject( OraDBSession ) Then
	Response.Write( "Ошибка: Не удалось создать объект OracleInProcServer.XOraSession. " )
Else
	Response.Write( "OK. " )
End If

Response.Write( "OpenDatabase: " )
Set OraDB = OraDBSession.OpenDatabase( Request("sDBName"), Request("DBUser") & "/" & Request("DBPass"), 0 )
If Err.Number <> 0 Then
	Response.Write( "Ошибка " & Err.Number & ": " & Err.Description & ". " )
ElseIf NOT IsObject( OraDB ) Then
	Response.Write( "Ошибка: Не удалось открыть БД. " )
Else
	Response.Write( "OK. " )
End If

Response.Write( "CreateDynaset: " )
Set DS = OraDB.CreateDynaset( "SELECT sysdate FROM dual", 0 )
If Err.Number <> 0 Then
	Response.Write( "Ошибка " & Err.Number & ": " & Err.Description & ". " )
ElseIf NOT IsObject( DS ) Then
	Response.Write( "Ошибка: Не удалось выполнить запрос. " )
Else
	Response.Write( "OK. " )
End If

%>