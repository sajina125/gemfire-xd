CREATE PROCEDURE SPROC_SELECT_DAP( 
IN ThreadName VARCHAR(20)
) 
LANGUAGE JAVA 
PARAMETER STYLE JAVA 
MODIFIES SQL DATA DYNAMIC RESULT SETS 0
EXTERNAL NAME 'examples.dap.sprocselect.SprocSelectProcedure.execute';